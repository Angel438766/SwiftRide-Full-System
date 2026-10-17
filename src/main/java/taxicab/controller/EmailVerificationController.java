package taxicab.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import taxicab.model.Passenger;
import taxicab.model.DriverApplication;
import taxicab.model.DriverRegistrationForm;
import taxicab.repository.DriverApplicationRepository;
import taxicab.service.EmailService;
import taxicab.service.OtpVerificationService;
import taxicab.service.PassengerService;
import taxicab.service.PasswordUtil;

@Controller
public class EmailVerificationController {

    private final OtpVerificationService otpService;
    private final PassengerService passengerService;
    private final DriverApplicationRepository driverRepository;

    public EmailVerificationController(
            OtpVerificationService otpService,
            PassengerService passengerService,
            DriverApplicationRepository driverRepository) {

        this.otpService = otpService;
        this.passengerService = passengerService;
        this.driverRepository = driverRepository;
    }

    @GetMapping("/verify-email")
    public String showVerificationPage(
            @RequestParam String role,
            HttpSession session,
            Model model) {

        String normalizedRole = role.trim().toUpperCase();

        if (!normalizedRole.equals("PASSENGER")
                && !normalizedRole.equals("DRIVER")) {
            return "redirect:/";
        }

        Object pending;

        if (normalizedRole.equals("PASSENGER")) {
            pending = session.getAttribute("pendingPassenger");
        } else {
            pending = session.getAttribute("pendingDriver");
        }

        if (pending == null) {
            model.addAttribute("error",
                    "Registration details were not found. Please register again.");
            return "redirect:" + (
                    normalizedRole.equals("PASSENGER")
                            ? "/passengers/register"
                            : "/driver-registration"
            );
        }

        model.addAttribute("role", normalizedRole);
        model.addAttribute("email",
                normalizedRole.equals("PASSENGER")
                        ? ((Passenger) pending).getEmail()
                        : ((DriverRegistrationForm) pending).getEmail());

        return "verify-email";
    }

    @PostMapping("/verify-email")
    public String verifyEmail(
            @RequestParam String role,
            @RequestParam String otp,
            HttpSession session,
            Model model) {

        String normalizedRole = role.trim().toUpperCase();

        if (!normalizedRole.equals("PASSENGER")
                && !normalizedRole.equals("DRIVER")) {
            return "redirect:/";
        }

        Object pending = session.getAttribute(
                normalizedRole.equals("PASSENGER")
                        ? "pendingPassenger"
                        : "pendingDriver"
        );

        if (pending == null) {
            return normalizedRole.equals("PASSENGER")
                    ? "redirect:/passengers/register"
                    : "redirect:/driver-registration";
        }

        String email = normalizedRole.equals("PASSENGER")
                ? ((Passenger) pending).getEmail()
                : ((DriverRegistrationForm) pending).getEmail();

        if (!otpService.verifyOtp(email, normalizedRole, otp)) {
            model.addAttribute("role", normalizedRole);
            model.addAttribute("email", email);
            model.addAttribute("error",
                    "Invalid or expired verification code. Please try again.");
            return "verify-email";
        }

        try {
            if (normalizedRole.equals("PASSENGER")) {
                Passenger passenger = (Passenger) pending;

                passengerService.registerPassenger(passenger);

                session.removeAttribute("pendingPassenger");

                return "redirect:/passengers/login?registered=1";
            }

            DriverRegistrationForm form = (DriverRegistrationForm) pending;

            if (driverRepository.findByEmailIgnoreCase(form.getEmail()).isPresent()) {
                session.removeAttribute("pendingDriver");
                model.addAttribute("error",
                        "This email is already registered as a driver applicant.");
                return "driver/register";
            }

            DriverApplication application = new DriverApplication();

            application.setFullName(form.getFullName().trim());
            application.setEmail(form.getEmail().trim().toLowerCase());
            application.setPhone(form.getPhone().trim());
            application.setPasswordHash(
                    PasswordUtil.hash(form.getPassword()));
            application.setLicenseNumber(form.getLicenseNumber().trim());
            application.setVehicleNumber(form.getVehicleNumber().trim());
            application.setVehicleCategory(form.getVehicleCategory().trim());
            application.setVehicleBrand(form.getVehicleBrand().trim());
            application.setVehicleModel(form.getVehicleModel().trim());
            application.setStatus("PENDING");
            application.setAvailability("OFFLINE");

            driverRepository.save(application);

            session.removeAttribute("pendingDriver");

            return "redirect:/driver-registration?success=1";

        } catch (IllegalArgumentException e) {
            model.addAttribute("role", normalizedRole);
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "verify-email";
        }
    }
}

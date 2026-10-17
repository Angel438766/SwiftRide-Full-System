package taxicab.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import taxicab.model.Passenger;
import taxicab.service.PassengerService;
import taxicab.service.EmailService;
import taxicab.service.OtpVerificationService;

import java.util.Optional;

@Controller
@RequestMapping("/passengers")
public class PassengerController {

    private final PassengerService passengerService;
    private final EmailService emailService;
    private final OtpVerificationService otpService;

    public PassengerController(
            PassengerService passengerService,
            EmailService emailService,
            OtpVerificationService otpService) {

        this.passengerService = passengerService;
        this.emailService = emailService;
        this.otpService = otpService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("passenger", new Passenger());
        return "passenger/register";
    }

    @PostMapping("/register")
    public String registerPassenger(
            @ModelAttribute("passenger") Passenger passenger,
            HttpSession session,
            Model model) {

        passenger.setEmail(passenger.getEmail() == null
                ? "" : passenger.getEmail().trim().toLowerCase());

        passenger.setName(passenger.getName() == null
                ? "" : passenger.getName().trim());

        passenger.setPhone(passenger.getPhone() == null
                ? "" : passenger.getPhone().trim());

        if (passenger.getName().isEmpty()
                || passenger.getEmail().isEmpty()
                || passenger.getPhone().isEmpty()
                || passenger.getPassword() == null
                || passenger.getPassword().length() < 6) {

            model.addAttribute("error",
                    "Please fill in all required fields. Password must contain at least 6 characters.");
            model.addAttribute("passenger", passenger);
            return "passenger/register";
        }

        if (!passenger.getEmail().matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            model.addAttribute("error", "Please enter a valid email address.");
            model.addAttribute("passenger", passenger);
            return "passenger/register";
        }

        try {
            String otp = otpService.generateOtp(
                    passenger.getEmail(), "PASSENGER");

            emailService.sendOtp(passenger.getEmail(), otp);

            session.setAttribute("pendingPassenger", passenger);

            return "redirect:/verify-email?role=PASSENGER";

        } catch (Exception e) {
            otpService.removeOtp(passenger.getEmail(), "PASSENGER");

            model.addAttribute("error",
                    "Unable to send verification email. Check your email settings and try again.");
            model.addAttribute("passenger", passenger);
            return "passenger/register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(required = false) String registered,
            @RequestParam(required = false) String deactivated,
            Model model) {

        if ("1".equals(registered)) {
            model.addAttribute("success",
                    "Email verified and registration completed. Please log in.");
        }

        if ("1".equals(deactivated)) {
            model.addAttribute("success",
                    "Your account has been deactivated.");
        }

        return "passenger/login";
    }

    @PostMapping("/login")
    public String loginPassenger(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        Optional<Passenger> result =
                passengerService.loginPassenger(
                        email.trim().toLowerCase(), password);

        if (result.isEmpty()) {
            model.addAttribute("error",
                    "Invalid email or password, or your account is inactive.");
            return "passenger/login";
        }

        Passenger passenger = result.get();

        session.setAttribute("loggedInPassengerId", passenger.getId());

        return "redirect:/passengers/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Object passengerId = session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        Passenger passenger = passengerService
                .getPassengerById((Long) passengerId)
                .orElse(null);

        if (passenger == null || !passenger.isActive()) {
            session.invalidate();
            return "redirect:/passengers/login";
        }

        model.addAttribute("passenger", passenger);
        return "passenger/dashboard";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Object passengerId = session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        Passenger passenger = passengerService
                .getPassengerById((Long) passengerId)
                .orElse(null);

        if (passenger == null || !passenger.isActive()) {
            session.invalidate();
            return "redirect:/passengers/login";
        }

        model.addAttribute("passenger", passenger);
        return "passenger/profile";
    }

    @PostMapping("/update")
    public String updatePassenger(
            @ModelAttribute("passenger") Passenger updatedPassenger,
            HttpSession session,
            Model model) {

        Object passengerId = session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        try {
            passengerService.updatePassenger(
                    (Long) passengerId, updatedPassenger);

            return "redirect:/passengers/profile?updated=1";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("passenger", updatedPassenger);
            return "passenger/profile";
        }
    }

    @GetMapping("/change-password")
    public String showChangePassword(HttpSession session) {
        if (session.getAttribute("loggedInPassengerId") == null) {
            return "redirect:/passengers/login";
        }

        return "passenger/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            Model model) {

        Object passengerId = session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        try {
            passengerService.changePassword(
                    (Long) passengerId,
                    currentPassword, newPassword, confirmPassword);

            model.addAttribute("success",
                    "Password changed successfully.");

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "passenger/change-password";
    }

    @PostMapping("/deactivate")
    public String deactivateAccount(HttpSession session) {
        Object passengerId = session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        passengerService.deactivatePassenger((Long) passengerId);
        session.invalidate();

        return "redirect:/passengers/login?deactivated=1";
    }

    @PostMapping("/delete")
    public String deleteAccount(HttpSession session) {
        Object passengerId = session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        passengerService.deletePassenger((Long) passengerId);
        session.invalidate();

        return "redirect:/";
    }

    @GetMapping
    public String listPassengers(Model model) {
        model.addAttribute("passengers",
                passengerService.getAllPassengers());

        return "passenger/list";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/passengers/login";
    }
}

package taxicab.controller;

import jakarta.servlet.http.HttpSession;

import taxicab.model.DriverApplication;
import taxicab.model.DriverRegistrationForm;
import taxicab.repository.DriverApplicationRepository;
import taxicab.service.EmailService;
import taxicab.service.OtpVerificationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class DriverRegistrationController {

    private final DriverApplicationRepository repository;
    private final EmailService emailService;
    private final OtpVerificationService otpService;

    public DriverRegistrationController(
            DriverApplicationRepository repository,
            EmailService emailService,
            OtpVerificationService otpService) {

        this.repository = repository;
        this.emailService = emailService;
        this.otpService = otpService;
    }

    @GetMapping("/driver-registration")
    public String showForm(
            @RequestParam(required = false) String success,
            Model model) {

        model.addAttribute("form", new DriverRegistrationForm());

        if ("1".equals(success)) {
            model.addAttribute("success",
                    "Email verified! Your application was submitted. Please wait for admin approval.");
        }

        return "driver/register";
    }

    @PostMapping("/driver-registration")
    public String register(
            @ModelAttribute("form") DriverRegistrationForm form,
            Model model,
            HttpSession session) {

        String name = clean(form.getFullName());
        String email = clean(form.getEmail()).toLowerCase();
        String phone = clean(form.getPhone());
        String password = form.getPassword();
        String license = clean(form.getLicenseNumber());

        String vehicleNumber = clean(form.getVehicleNumber());
        String category = clean(form.getVehicleCategory());
        String brand = clean(form.getVehicleBrand());
        String vehicleModel = clean(form.getVehicleModel());

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()
                || password == null || password.trim().isEmpty()
                || license.isEmpty() || vehicleNumber.isEmpty()
                || category.isEmpty() || brand.isEmpty()
                || vehicleModel.isEmpty()) {

            model.addAttribute("error",
                    "Please fill in every field, including vehicle brand and model.");
            return "driver/register";
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            model.addAttribute("error",
                    "Please enter a valid email address.");
            return "driver/register";
        }

        if (password.length() < 8) {
            model.addAttribute("error",
                    "Password must contain at least 8 characters.");
            return "driver/register";
        }

        if (!category.equals("Tuk Tuk")
                && !category.equals("Mini")
                && !category.equals("Car")
                && !category.equals("Van")
                && !category.equals("Premium")) {

            model.addAttribute("error",
                    "Please select a valid vehicle category.");
            return "driver/register";
        }

        if (repository.findByEmailIgnoreCase(email).isPresent()) {
            model.addAttribute("error",
                    "This email is already registered as a driver applicant.");
            return "driver/register";
        }

        form.setFullName(name);
        form.setEmail(email);
        form.setPhone(phone);
        form.setLicenseNumber(license);
        form.setVehicleNumber(vehicleNumber);
        form.setVehicleCategory(category);
        form.setVehicleBrand(brand);
        form.setVehicleModel(vehicleModel);

        String otp = otpService.generateOtp(email, "DRIVER");

        try {
            emailService.sendOtp(email, otp);

            session.setAttribute("pendingDriver", form);

            return "redirect:/verify-email?role=DRIVER";

        } catch (Exception e) {
            otpService.removeOtp(email, "DRIVER");

            model.addAttribute("form", form);
            model.addAttribute("error",
                    "Could not send the verification email. Please check the email configuration and try again.");

            return "driver/register";
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

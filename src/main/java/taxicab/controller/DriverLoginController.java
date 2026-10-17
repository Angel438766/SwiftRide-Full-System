package taxicab.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import taxicab.model.Driver;
import taxicab.repository.DriverRepository;
import taxicab.service.PasswordUtil;

@Controller
public class DriverLoginController {

    private final DriverRepository driverRepository;

    public DriverLoginController(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @GetMapping("/driver-login")
    public String showLoginPage() {
        return "driver/login";
    }

    @PostMapping("/driver-login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        Driver driver = driverRepository.findAll()
                .stream()
                .filter(d -> d.getEmail() != null
                        && d.getEmail().equalsIgnoreCase(email.trim()))
                .filter(d -> PasswordUtil.matches(
                        password,
                        d.getDriverPassword() == null
                                ? ""
                                : d.getDriverPassword()))
                .findFirst()
                .orElse(null);
        
        if (driver == null) {
            model.addAttribute(
                    "error",
                    "Invalid email or password."
            );
            return "driver/login";
        }

        if (!driver.isApproved()) {
            model.addAttribute(
                    "error",
                    "Your driver account has not been approved yet."
            );
            return "driver/login";
        }

        session.setAttribute("loggedInDriverId", driver.getId());

        return "redirect:/driver-dashboard";
    }

    @GetMapping("/driver-logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/driver-login";
    }
}

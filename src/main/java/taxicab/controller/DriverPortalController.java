package taxicab.controller;

import taxicab.model.DriverApplication;
import taxicab.repository.DriverApplicationRepository;
import taxicab.service.PasswordUtil;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/driver-portal")
public class DriverPortalController {

    private final DriverApplicationRepository repository;

    public DriverPortalController(DriverApplicationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "driver/portal-login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        DriverApplication driver = repository
                .findByEmailIgnoreCase(email.trim())
                .orElse(null);

        if (driver == null) {
            model.addAttribute("error",
                    "No driver account was found with that email.");
            return "driver/portal-login";
        }

        if (!"APPROVED".equals(driver.getStatus())) {
            model.addAttribute("error",
                    "Your application must be approved before you can log in.");
            return "driver/portal-login";
        }

        if (!PasswordUtil.matches(password, driver.getPasswordHash())) {
            model.addAttribute("error", "Incorrect password.");
            return "driver/portal-login";
        }

        session.setAttribute("swiftRideDriverId", driver.getId());

        return "redirect:/driver-portal/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {

        Long driverId = (Long) session.getAttribute("swiftRideDriverId");

        if (driverId == null) {
            return "redirect:/driver-portal/login";
        }

        DriverApplication driver = repository.findById(driverId).orElse(null);

        if (driver == null || !"APPROVED".equals(driver.getStatus())) {
            session.removeAttribute("swiftRideDriverId");
            return "redirect:/driver-portal/login";
        }

        model.addAttribute("driver", driver);

        return "driver/portal-dashboard";
    }

    @PostMapping("/availability")
    public String changeAvailability(
            @RequestParam boolean online,
            HttpSession session) {

        Long driverId = (Long) session.getAttribute("swiftRideDriverId");

        if (driverId == null) {
            return "redirect:/driver-portal/login";
        }

        DriverApplication driver = repository.findById(driverId).orElse(null);

        if (driver == null || !"APPROVED".equals(driver.getStatus())) {
            session.removeAttribute("swiftRideDriverId");
            return "redirect:/driver-portal/login";
        }

        driver.setAvailability(online ? "ONLINE" : "OFFLINE");
        repository.save(driver);

        return "redirect:/driver-portal/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("swiftRideDriverId");
        return "redirect:/driver-portal/login";
    }
}
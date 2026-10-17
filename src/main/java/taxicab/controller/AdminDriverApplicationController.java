package taxicab.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import taxicab.model.Driver;
import taxicab.model.DriverApplication;
import taxicab.model.Vehicle;
import taxicab.repository.DriverApplicationRepository;
import taxicab.repository.DriverRepository;
import taxicab.repository.VehicleRepository;

@Controller
@RequestMapping("/admin")
public class AdminDriverApplicationController {

    private final DriverApplicationRepository applicationRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    @Value("${swiftride.admin.username:admin}")
    private String adminUsername;

    @Value("${swiftride.admin.password:ChangeMe123!}")
    private String adminPassword;

    public AdminDriverApplicationController(
            DriverApplicationRepository applicationRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository) {

        this.applicationRepository = applicationRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // Admin login page
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (isAdmin(session)) {
            return "redirect:/admin/dashboard";
        }
        return "admin/login";
    }

    // Process admin login
    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        if (adminUsername.equals(username.trim())
                && adminPassword.equals(password)) {

            session.setAttribute("swiftRideAdminLoggedIn", true);
            return "redirect:/admin/dashboard";
        }

        model.addAttribute(
                "error",
                "Incorrect admin username or password."
        );

        return "admin/login";
    }

    // Admin dashboard
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }
        return "admin/dashboard";
    }

    // View driver applications
    @GetMapping("/driver-applications")
    public String applications(
            HttpSession session,
            Model model) {

        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        model.addAttribute(
                "applications",
                applicationRepository.findAllByOrderByIdDesc()
        );

        return "admin/driver-applications";
    }

    // Approve driver application
    @PostMapping("/driver-applications/{id}/approve")
    public String approve(
            @PathVariable Long id,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        DriverApplication application = applicationRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver application not found"
                        ));

        if (!"PENDING".equals(application.getStatus())) {
            return "redirect:/admin/driver-applications";
        }

        Driver driver = driverRepository.findAll()
                .stream()
                .filter(d -> d.getEmail() != null
                        && d.getEmail().equalsIgnoreCase(
                        application.getEmail()))
                .findFirst()
                .orElse(null);

        if (driver == null) {
            driver = new Driver();
            driver.setName(application.getFullName());
            driver.setEmail(application.getEmail());
            driver.setPhone(application.getPhone());
            driver.setDriverPassword(application.getPasswordHash());
        }

        driver.setApproved(true);
        driver.setOnline(false);
        driver.setAvailable(false);

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber(application.getVehicleNumber());
        vehicle.setVehicleType(application.getVehicleCategory());
        vehicle.setBrand(application.getVehicleBrand());
        vehicle.setModel(application.getVehicleModel());
        vehicle.setActive(true);

        String category = application.getVehicleCategory();

        if ("Tuk Tuk".equalsIgnoreCase(category)) {
            vehicle.setSeats(3);
        } else if ("Mini".equalsIgnoreCase(category)) {
            vehicle.setSeats(4);
        } else if ("Van".equalsIgnoreCase(category)) {
            vehicle.setSeats(10);
        } else {
            vehicle.setSeats(4);
        }

        vehicle = vehicleRepository.save(vehicle);

        driver.setVehicle(vehicle);
        driverRepository.save(driver);

        application.setStatus("APPROVED");
        applicationRepository.save(application);

        return "redirect:/admin/driver-applications";
    }

    // Reject driver application
    @PostMapping("/driver-applications/{id}/reject")
    public String reject(
            @PathVariable Long id,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        DriverApplication application = applicationRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver application not found"
                        ));

        if ("PENDING".equals(application.getStatus())) {
            application.setStatus("REJECTED");
            applicationRepository.save(application);
        }

        return "redirect:/admin/driver-applications";
    }

    // Admin logout
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(
                session.getAttribute("swiftRideAdminLoggedIn")
        );
    }
}
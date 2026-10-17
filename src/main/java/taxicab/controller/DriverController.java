
package taxicab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import taxicab.model.Driver;
import taxicab.service.DriverService;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    // View all drivers
    @GetMapping
    public String listDrivers(Model model) {
        model.addAttribute("drivers",
                driverService.getAllDrivers());
        return "driver/list";
    }

    // Open form to add a driver
    @GetMapping("/new")
    public String showDriverForm(Model model) {
        model.addAttribute("driver", new Driver());
        return "driver/form";
    }

    // Open form to edit a driver
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        Driver driver = driverService.getDriverById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver not found: " + id));

        model.addAttribute("driver", driver);
        return "driver/form";
    }

    // Save or update driver
    @PostMapping("/save")
    public String saveDriver(@ModelAttribute Driver driver) {
        driverService.saveDriver(driver);
        return "redirect:/drivers";
    }

    // Delete driver
    @PostMapping("/delete/{id}")
    public String deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return "redirect:/drivers";
    }

    // Approve driver
    @PostMapping("/approve/{id}")
    public String approveDriver(@PathVariable Long id) {
        driverService.approveDriver(id, true);
        return "redirect:/drivers";
    }

    // Reject driver
    @PostMapping("/reject/{id}")
    public String rejectDriver(@PathVariable Long id) {
        driverService.approveDriver(id, false);
        return "redirect:/drivers";
    }

    // Set driver online
    @PostMapping("/online/{id}")
    public String setDriverOnline(@PathVariable Long id) {
        driverService.updateOnlineStatus(id, true);
        return "redirect:/drivers";
    }

    // Set driver offline
    @PostMapping("/offline/{id}")
    public String setDriverOffline(@PathVariable Long id) {
        driverService.updateOnlineStatus(id, false);
        return "redirect:/drivers";
    }

    // Mark driver available
    @PostMapping("/available/{id}")
    public String setDriverAvailable(@PathVariable Long id) {
        driverService.updateAvailability(id, true);
        return "redirect:/drivers";
    }

    // Mark driver unavailable
    @PostMapping("/unavailable/{id}")
    public String setDriverUnavailable(@PathVariable Long id) {
        driverService.updateAvailability(id, false);
        return "redirect:/drivers";
    }

    // Open driver GPS location sharing page
    @GetMapping("/location-page")
    public String showLocationPage() {
        return "driver/location";
    }

    // Update driver's GPS location
    @ResponseBody
    @PostMapping("/{id}/location")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @PathVariable Long id,
            @RequestBody Map<String, Double> location) {

        Double latitude = location.get("latitude");
        Double longitude = location.get("longitude");

        if (latitude == null || longitude == null
                || !Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {

            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid GPS coordinates"));
        }

        Driver driver = driverService.updateDriverLocation(
                id, latitude, longitude);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Location updated successfully");
        response.put("latitude", driver.getLatitude());
        response.put("longitude", driver.getLongitude());

        return ResponseEntity.ok(response);
    }

    // Get driver's latest GPS location
    @ResponseBody
    @GetMapping("/{id}/location")
    public ResponseEntity<Map<String, Object>> getLocation(
            @PathVariable Long id) {

        Driver driver = driverService.getDriverById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver not found: " + id));

        Map<String, Object> response = new HashMap<>();

        response.put("driverId", driver.getId());
        response.put("driverName", driver.getName());
        response.put("latitude", driver.getLatitude());
        response.put("longitude", driver.getLongitude());
        response.put("online", driver.isOnline());

        return ResponseEntity.ok(response);
    }
}

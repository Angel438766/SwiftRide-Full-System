
package taxicab.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import taxicab.model.Booking;
import taxicab.service.BookingService;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public String listBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "booking/list";
    }

    @GetMapping("/new")
    public String showBookingForm(Model model, HttpSession session) {
        Long passengerId =
                (Long) session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        model.addAttribute("booking", new Booking());
        return "booking/form";
    }

    @PostMapping("/save")
    public String saveBooking(
            @ModelAttribute("booking") Booking booking,
            HttpSession session) {

        Long passengerId =
                (Long) session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        // Save as PENDING. A driver must accept it manually.
        bookingService.saveBookingForPassenger(booking, passengerId);

        return "redirect:/booking-history";
    }

    @GetMapping("/assign/{id}")
    public String showAssignForm(
            @PathVariable Long id,
            Model model) {

        var booking = bookingService.getBookingById(id);

        if (booking.isEmpty()) {
            return "redirect:/bookings";
        }

        model.addAttribute("booking", booking.get());
        model.addAttribute(
                "availableDrivers",
                bookingService.getAvailableDrivers());

        return "booking/assign";
    }

    @PostMapping("/assign/{id}")
    public String assignDriver(
            @PathVariable Long id,
            @RequestParam Long driverId,
            Model model) {

        boolean assigned = bookingService.assignDriver(id, driverId);

        if (!assigned) {
            var booking = bookingService.getBookingById(id);

            if (booking.isEmpty()) {
                return "redirect:/bookings";
            }

            model.addAttribute("booking", booking.get());
            model.addAttribute(
                    "availableDrivers",
                    bookingService.getAvailableDrivers());
            model.addAttribute(
                    "errorMessage",
                    "Driver assignment failed. Check booking status and driver availability.");

            return "booking/assign";
        }

        return "redirect:/bookings";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            HttpSession session) {

        // Driver actions must only affect their own assigned booking.
        Object driverSessionId = session.getAttribute("loggedInDriverId");

        if (driverSessionId instanceof Long driverId) {
            var optionalBooking = bookingService.getBookingById(id);

            if (optionalBooking.isEmpty()
                    || optionalBooking.get().getDriverId() == null
                    || !driverId.equals(optionalBooking.get().getDriverId())) {
                return "redirect:/driver-dashboard";
            }

            if (!"STARTED".equals(status)
                    && !"COMPLETED".equals(status)) {
                return "redirect:/driver-dashboard";
            }

            bookingService.updateStatus(id, status);
            return "redirect:/driver-dashboard";
        }

        // Preserve the existing non-driver management workflow.
        if ("PENDING".equals(status)
                || "ACCEPTED".equals(status)
                || "STARTED".equals(status)
                || "COMPLETED".equals(status)
                || "CANCELLED".equals(status)) {
            bookingService.updateStatus(id, status);
        }

        return "redirect:/bookings";
    }

    @PostMapping("/cancel/{id}")
    public String cancelBooking(
            @PathVariable Long id,
            @RequestParam String reason,
            HttpSession session) {

        Long passengerId =
                (Long) session.getAttribute("loggedInPassengerId");

        if (passengerId != null) {
            var optionalBooking = bookingService.getBookingById(id);

            if (optionalBooking.isEmpty()
                    || !passengerId.equals(
                    optionalBooking.get().getPassengerId())) {
                return "redirect:/booking-history";
            }

            bookingService.cancelBooking(id, reason);
            return "redirect:/booking-history";
        }

        bookingService.cancelBooking(id, reason);
        return "redirect:/bookings";
    }

    @PostMapping("/delete/{id}")
    public String deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return "redirect:/bookings";
    }

    @GetMapping("/track-driver")
    public String trackDriverPage() {
        return "booking/track-driver";
    }

    @ResponseBody
    @GetMapping("/track-data/{id}")
    public ResponseEntity<Map<String, Object>> getTrackingData(
            @PathVariable Long id,
            HttpSession session) {

        Long passengerId =
                (Long) session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Please log in first."));
        }

        var optionalBooking = bookingService.getBookingById(id);

        if (optionalBooking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Booking booking = optionalBooking.get();

        if (!passengerId.equals(booking.getPassengerId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "You cannot track this booking."));
        }

        Map<String, Object> data = new HashMap<>();

        data.put("bookingId", booking.getId());
        data.put("status", booking.getStatus());
        data.put("pickupLocation", booking.getPickupLocation());
        data.put("destination", booking.getDestination());
        data.put("distanceKm", booking.getDistanceKm());
        data.put("estimatedMinutes", booking.getEstimatedMinutes());
        data.put("fare", booking.getFare());
        data.put("driverId", booking.getDriverId());

        if (booking.getDriverId() != null) {
            bookingService.getDriverById(booking.getDriverId())
                    .ifPresent(driver -> {
                        data.put("driverName", driver.getName());
                        data.put("driverPhone", driver.getPhone());
                        data.put("driverLatitude", driver.getLatitude());
                        data.put("driverLongitude", driver.getLongitude());
                        data.put("driverOnline", driver.isOnline());
                    });
        }

        return ResponseEntity.ok(data);
    }
}


package taxicab.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import taxicab.model.Booking;
import taxicab.model.Driver;
import taxicab.repository.BookingRepository;
import taxicab.service.BookingService;
import taxicab.service.DriverService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/driver-dashboard")
public class DriverDashboardController {

    private final BookingRepository bookingRepository;
    private final DriverService driverService;
    private final BookingService bookingService;

    public DriverDashboardController(
            BookingRepository bookingRepository,
            DriverService driverService,
            BookingService bookingService) {

        this.bookingRepository = bookingRepository;
        this.driverService = driverService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String showDashboard(
            HttpSession session,
            Model model) {

        Object driverIdObject =
                session.getAttribute("loggedInDriverId");

        if (!(driverIdObject instanceof Long)) {
            return "redirect:/driver-login";
        }

        Long driverId = (Long) driverIdObject;

        Driver driver = driverService
                .getDriverById(driverId)
                .orElse(null);

        if (driver == null) {
            session.removeAttribute("loggedInDriverId");
            return "redirect:/driver-login";
        }

        List<Booking> pendingBookings =
                bookingRepository
                        .findByStatusOrderByBookingDateDesc("PENDING");

        List<Booking> myBookings =
                bookingRepository.findByDriverId(driverId);

        model.addAttribute("driver", driver);
        model.addAttribute("pendingBookings", pendingBookings);
        model.addAttribute("myBookings", myBookings);

        return "driver/dashboard";
    }

    @PostMapping("/accept/{bookingId}")
    public String acceptBooking(
            @PathVariable Long bookingId,
            HttpSession session) {

        Object driverIdObject =
                session.getAttribute("loggedInDriverId");

        if (!(driverIdObject instanceof Long)) {
            return "redirect:/driver-login";
        }

        Long driverId = (Long) driverIdObject;

        var optionalBooking =
                bookingService.getBookingById(bookingId);

        if (optionalBooking.isEmpty()) {
            return "redirect:/driver-dashboard";
        }

        Booking booking = optionalBooking.get();

        if (!"PENDING".equals(booking.getStatus())) {
            return "redirect:/driver-dashboard";
        }

        // BookingService checks driver availability and assigns the driver.
        bookingService.assignDriver(bookingId, driverId);

        return "redirect:/driver-dashboard";
    }

    @PostMapping("/reject/{bookingId}")
    public String rejectBooking(
            @PathVariable Long bookingId,
            HttpSession session) {

        Object driverIdObject =
                session.getAttribute("loggedInDriverId");

        if (!(driverIdObject instanceof Long)) {
            return "redirect:/driver-login";
        }

        var optionalBooking =
                bookingService.getBookingById(bookingId);

        if (optionalBooking.isEmpty()) {
            return "redirect:/driver-dashboard";
        }

        Booking booking = optionalBooking.get();

        if ("PENDING".equals(booking.getStatus())) {
            booking.setStatus("REJECTED");
            bookingRepository.save(booking);
        }

        return "redirect:/driver-dashboard";
    }
}

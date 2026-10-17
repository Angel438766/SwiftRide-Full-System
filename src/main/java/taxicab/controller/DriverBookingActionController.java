package taxicab.controller;

import taxicab.model.Booking;
import taxicab.repository.BookingRepository;
import taxicab.service.TripService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/driver-bookings")
public class DriverBookingActionController {

    private final BookingRepository bookingRepository;
    private final TripService tripService;

    public DriverBookingActionController(
            BookingRepository bookingRepository,
            TripService tripService) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
    }

    @GetMapping
    public String showBookings(Model model) {
        model.addAttribute(
                "bookings", bookingRepository.findAll());
        return "driver-bookings";
    }

    @PostMapping("/accept/{id}")
    public String acceptBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found"));

        if ("PENDING".equals(booking.getStatus())) {
            booking.setStatus("ACCEPTED");
            bookingRepository.save(booking);

            // Automatically create the related Trip
            tripService.createTripFromAcceptedBooking(booking);
        }

        return "redirect:/driver-bookings";
    }

    @PostMapping("/reject/{id}")
    public String rejectBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found"));

        if ("PENDING".equals(booking.getStatus())) {
            booking.setStatus("REJECTED");
            bookingRepository.save(booking);
        }

        return "redirect:/driver-bookings";
    }
}

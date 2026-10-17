
package taxicab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import taxicab.model.Booking;
import taxicab.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/scheduled-bookings")
public class ScheduledBookingController {

    private final BookingRepository bookingRepository;

    public ScheduledBookingController(
            BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("booking", new Booking());
        return "scheduled-booking-form";
    }

    @PostMapping("/save")
    public String saveBooking(
            @RequestParam String passengerName,
            @RequestParam String pickupLocation,
            @RequestParam String destination,
            @RequestParam String vehicleCategory,
            @RequestParam String scheduledTime,
            Model model) {

        model.addAttribute("booking", new Booking());

        // Validate required fields
        if (passengerName == null || passengerName.trim().isEmpty()
                || pickupLocation == null || pickupLocation.trim().isEmpty()
                || destination == null || destination.trim().isEmpty()
                || vehicleCategory == null || vehicleCategory.trim().isEmpty()
                || scheduledTime == null || scheduledTime.trim().isEmpty()) {

            model.addAttribute("error",
                    "All fields are required. Please complete the form.");

            return "scheduled-booking-form";
        }

        // Validate pickup and destination
        if (pickupLocation.trim().equalsIgnoreCase(destination.trim())) {
            model.addAttribute("error",
                    "Pickup location and destination must be different.");

            return "scheduled-booking-form";
        }

        // Validate vehicle category
        List<String> validCategories = List.of(
                "Tuk Tuk", "Mini", "Car", "Van", "Premium"
        );

        boolean validCategory = validCategories.stream()
                .anyMatch(category ->
                        category.equalsIgnoreCase(vehicleCategory.trim()));

        if (!validCategory) {
            model.addAttribute("error",
                    "Please select a valid vehicle category.");

            return "scheduled-booking-form";
        }

        try {
            LocalDateTime time = LocalDateTime.parse(scheduledTime);

            if (!time.isAfter(LocalDateTime.now())) {
                model.addAttribute("error",
                        "Please select a future date and time.");

                return "scheduled-booking-form";
            }

            Booking booking = new Booking();

            booking.setPassengerName(passengerName.trim());
            booking.setPickupLocation(pickupLocation.trim());
            booking.setDestination(destination.trim());

            String selectedCategory = validCategories.stream()
                    .filter(category ->
                            category.equalsIgnoreCase(vehicleCategory.trim()))
                    .findFirst()
                    .orElseThrow();

            booking.setVehicleCategory(selectedCategory);
            booking.setScheduledTime(time);
            booking.setBookingDate(LocalDateTime.now());
            booking.setStatus("PENDING");
            booking.setFare(0.0);

            bookingRepository.save(booking);

            return "redirect:/scheduled-bookings/list";

        } catch (Exception e) {
            model.addAttribute("error",
                    "Invalid date or time. Please check your details.");

            return "scheduled-booking-form";
        }
    }

    @GetMapping("/list")
    public String showScheduledBookings(Model model) {

        List<Booking> scheduledBookings = bookingRepository.findAll()
                .stream()
                .filter(booking -> booking.getScheduledTime() != null)
                .sorted((a, b) ->
                        a.getScheduledTime().compareTo(b.getScheduledTime()))
                .collect(Collectors.toList());

        model.addAttribute("scheduledBookings", scheduledBookings);

        return "scheduled-booking-list";
    }
}


package taxicab.controller;

import taxicab.model.Booking;
import taxicab.repository.BookingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/booking-update")
public class BookingUpdateController {

    private final BookingRepository bookingRepository;

    public BookingUpdateController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public String showBookings(Model model) {
        model.addAttribute("bookings", bookingRepository.findAll());
        return "booking-update-list";
    }

    @GetMapping("/edit/{id}")
    public String editBooking(@PathVariable Long id, Model model) {
        Booking booking = bookingRepository.findById(id).orElse(null);

        if (booking == null) {
            model.addAttribute("error", "Booking not found.");
            model.addAttribute("bookings", bookingRepository.findAll());
            return "booking-update-list";
        }

        model.addAttribute("booking", booking);
        return "booking-update-form";
    }

    @PostMapping("/save/{id}")
    public String saveBooking(
            @PathVariable Long id,
            @RequestParam String passengerName,
            @RequestParam String pickupLocation,
            @RequestParam String destination,
            @RequestParam String vehicleCategory,
            Model model) {

        Booking booking = bookingRepository.findById(id).orElse(null);

        if (booking == null) {
            return "redirect:/booking-update";
        }

        if (passengerName == null || passengerName.trim().isEmpty()
                || pickupLocation == null || pickupLocation.trim().isEmpty()
                || destination == null || destination.trim().isEmpty()
                || vehicleCategory == null || vehicleCategory.trim().isEmpty()) {

            model.addAttribute("booking", booking);
            model.addAttribute("error", "All fields are required.");
            return "booking-update-form";
        }

        if (pickupLocation.trim().equalsIgnoreCase(destination.trim())) {
            model.addAttribute("booking", booking);
            model.addAttribute("error",
                    "Pickup and destination must be different.");
            return "booking-update-form";
        }

        List<String> validCategories =
                List.of("Tuk Tuk", "Mini", "Car", "Van", "Premium");

        String selectedCategory = validCategories.stream()
                .filter(category ->
                        category.equalsIgnoreCase(vehicleCategory.trim()))
                .findFirst()
                .orElse(null);

        if (selectedCategory == null) {
            model.addAttribute("booking", booking);
            model.addAttribute("error", "Please select a valid vehicle category.");
            return "booking-update-form";
        }

        booking.setPassengerName(passengerName.trim());
        booking.setPickupLocation(pickupLocation.trim());
        booking.setDestination(destination.trim());
        booking.setVehicleCategory(selectedCategory);

        bookingRepository.save(booking);

        return "redirect:/booking-update";
    }
}

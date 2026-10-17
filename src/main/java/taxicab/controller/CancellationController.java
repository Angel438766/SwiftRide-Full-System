package taxicab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import taxicab.model.Booking;
import taxicab.model.Cancellation;
import taxicab.service.BookingService;
import taxicab.service.CancellationService;

@Controller
@RequestMapping("/cancellations")
public class CancellationController {

    private final CancellationService cancellationService;
    private final BookingService bookingService;

    public CancellationController(
            CancellationService cancellationService,
            BookingService bookingService) {
        this.cancellationService = cancellationService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String listCancellations(Model model) {
        model.addAttribute(
                "cancellations",
                cancellationService.getAllCancellations());

        return "cancellation/list";
    }

    @GetMapping("/new")
    public String showCancellationForm(
            @RequestParam(required = false) Long bookingId,
            Model model) {

        Cancellation cancellation = new Cancellation();

        if (bookingId != null) {
            Booking booking = bookingService.getBookingById(bookingId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Booking not found: " + bookingId));

            if ("CANCELLED".equals(booking.getStatus())
                    || "COMPLETED".equals(booking.getStatus())) {
                throw new IllegalStateException(
                        "This booking cannot be cancelled.");
            }

            cancellation.setBookingId(bookingId);
        }

        model.addAttribute("cancellation", cancellation);
        return "cancellation/form";
    }

    @PostMapping("/save")
    public String saveCancellation(
            @RequestParam Long bookingId,
            @RequestParam String reason) {

        cancellationService.cancelBooking(bookingId, reason);
        return "redirect:/cancellations";
    }

    @GetMapping("/delete/{id}")
    public String deleteCancellation(@PathVariable Long id) {
        cancellationService.deleteCancellation(id);
        return "redirect:/cancellations";
    }
}


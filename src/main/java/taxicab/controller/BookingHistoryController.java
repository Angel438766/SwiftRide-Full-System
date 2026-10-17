
package taxicab.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import taxicab.service.BookingService;

@Controller
@RequestMapping("/booking-history")
public class BookingHistoryController {

    private final BookingService bookingService;

    public BookingHistoryController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public String showBookingHistory(HttpSession session, Model model) {

        Long passengerId =
                (Long) session.getAttribute("loggedInPassengerId");

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        model.addAttribute(
                "bookings",
                bookingService.getBookingsByPassengerId(passengerId)
        );

        return "booking/history";
    }
}

package taxicab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import taxicab.model.Trip;
import taxicab.service.TripService;

@Controller
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public String listTrips(Model model) {
        model.addAttribute("trips", tripService.getAllTrips());
        return "trip/list";
    }

    @GetMapping("/new")
    public String showTripForm(Model model) {
        model.addAttribute("trip", new Trip());
        return "trip/form";
    }

    @PostMapping("/save")
    public String saveTrip(@ModelAttribute Trip trip) {
        tripService.saveTrip(trip);
        return "redirect:/trips";
    }

    @GetMapping("/edit/{id}")
    public String editTrip(@PathVariable Long id, Model model) {
        Trip trip = tripService.getTripById(id)
                .orElseThrow(() ->
                        new RuntimeException("Trip not found: " + id));

        model.addAttribute("trip", trip);
        return "trip/form";
    }

    @PostMapping("/start/{id}")
    public String startTrip(@PathVariable Long id) {
        tripService.startTrip(id);
        return "redirect:/trips";
    }

    @PostMapping("/complete/{id}")
    public String completeTrip(@PathVariable Long id) {
        tripService.completeTrip(id);
        return "redirect:/trips";
    }

    @GetMapping("/delete/{id}")
    public String deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return "redirect:/trips";
    }
}
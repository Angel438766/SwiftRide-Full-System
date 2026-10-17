package taxicab.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import taxicab.model.FavouriteLocation;
import taxicab.service.FavouriteLocationService;

@Controller
@RequestMapping("/favourite-locations")
public class FavouriteLocationController {

    private final FavouriteLocationService service;

    public FavouriteLocationController(
            FavouriteLocationService service) {
        this.service = service;
    }

    private Long getPassengerId(HttpSession session) {
        Object id = session.getAttribute("loggedInPassengerId");

        if (id instanceof Long) {
            return (Long) id;
        }

        return null;
    }

    @GetMapping
    public String listLocations(
            HttpSession session, Model model) {

        Long passengerId = getPassengerId(session);

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        model.addAttribute(
                "locations",
                service.getLocationsForPassenger(passengerId));

        return "favourite-location/list";
    }

    @GetMapping("/new")
    public String showAddForm(
            HttpSession session, Model model) {

        if (getPassengerId(session) == null) {
            return "redirect:/passengers/login";
        }

        model.addAttribute("location", new FavouriteLocation());
        model.addAttribute("pageTitle", "Add Favourite Location");

        return "favourite-location/form";
    }

    @PostMapping("/save")
    public String saveLocation(
            @ModelAttribute("location") FavouriteLocation location,
            HttpSession session,
            Model model) {

        Long passengerId = getPassengerId(session);

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        try {
            service.saveLocation(passengerId, location);
            return "redirect:/favourite-locations";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("location", location);
            model.addAttribute("pageTitle", "Add Favourite Location");
            return "favourite-location/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        Long passengerId = getPassengerId(session);

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        FavouriteLocation location =
                service.getLocationForPassenger(id, passengerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Favourite location not found"));

        model.addAttribute("location", location);
        model.addAttribute("pageTitle", "Edit Favourite Location");

        return "favourite-location/form";
    }

    @PostMapping("/update/{id}")
    public String updateLocation(
            @PathVariable Long id,
            @ModelAttribute("location") FavouriteLocation location,
            HttpSession session) {

        Long passengerId = getPassengerId(session);

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        service.updateLocation(id, passengerId, location);

        return "redirect:/favourite-locations";
    }

    @PostMapping("/delete/{id}")
    public String deleteLocation(
            @PathVariable Long id,
            HttpSession session) {

        Long passengerId = getPassengerId(session);

        if (passengerId == null) {
            return "redirect:/passengers/login";
        }

        service.deleteLocation(id, passengerId);

        return "redirect:/favourite-locations";
    }
}

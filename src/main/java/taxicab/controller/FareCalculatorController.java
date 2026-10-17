package taxicab.controller;

import taxicab.service.FareCalculationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/fare-calculator")
public class FareCalculatorController {

    private final FareCalculationService fareService;

    public FareCalculatorController(
            FareCalculationService fareService) {
        this.fareService = fareService;
    }

    @GetMapping
    public String showCalculator() {
        return "fare-calculator";
    }

    @GetMapping("/calculate")
    @ResponseBody
    public Map<String, Object> calculateFare(
            @RequestParam String vehicleCategory,
            @RequestParam double distanceKm) {

        Map<String, Object> result = new HashMap<>();

        try {
            double fare = fareService.calculateFare(
                    vehicleCategory, distanceKm);

            result.put("success", true);
            result.put("distanceKm", distanceKm);
            result.put("fare", fare);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }
}

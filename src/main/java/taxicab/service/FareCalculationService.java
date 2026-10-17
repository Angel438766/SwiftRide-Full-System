package taxicab.service;

import org.springframework.stereotype.Service;

@Service
public class FareCalculationService {

    private static final double BASE_FARE = 100.0;

    public double calculateFare(
            String vehicleCategory,
            double distanceKm) {

        if (distanceKm < 0) {
            throw new IllegalArgumentException(
                    "Distance cannot be negative.");
        }

        double ratePerKm;

        if (vehicleCategory == null) {
            throw new IllegalArgumentException(
                    "Vehicle category is required.");
        }

        switch (vehicleCategory.trim().toLowerCase()) {
            case "tuk tuk":
            case "tuktuk":
                ratePerKm = 80.0;
                break;

            case "mini":
                ratePerKm = 100.0;
                break;

            case "car":
                ratePerKm = 120.0;
                break;

            case "van":
                ratePerKm = 160.0;
                break;

            case "premium":
                ratePerKm = 250.0;
                break;

            default:
                throw new IllegalArgumentException(
                        "Invalid vehicle category.");
        }

        double fare = BASE_FARE + (distanceKm * ratePerKm);

        return Math.round(fare * 100.0) / 100.0;
    }
}


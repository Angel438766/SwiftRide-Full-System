package taxicab.service;

import org.springframework.stereotype.Service;
import taxicab.model.Driver;
import taxicab.repository.DriverRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    // View all drivers
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    // Find driver by ID
    public Optional<Driver> getDriverById(Long id) {
        return driverRepository.findById(id);
    }

    // Add a new driver or update an existing driver
    public Driver saveDriver(Driver driver) {
        if (driver.getId() == null) {
            driver.setApproved(false);
            driver.setOnline(false);
            driver.setAvailable(false);
        }

        return driverRepository.save(driver);
    }

    // Delete driver
    public void deleteDriver(Long id) {
        driverRepository.deleteById(id);
    }

    // Get drivers ready to accept bookings
    public List<Driver> getAvailableDrivers() {
        return driverRepository
                .findByApprovedTrueAndOnlineTrueAndAvailableTrue();
    }

    // Approve or reject driver
    public void approveDriver(Long id, boolean approved) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Driver not found"));

        driver.setApproved(approved);

        if (!approved) {
            driver.setOnline(false);
            driver.setAvailable(false);
        }

        driverRepository.save(driver);
    }

    // Set driver online or offline
    public void updateOnlineStatus(Long id, boolean online) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Driver not found"));

        if (online && !driver.isApproved()) {
            throw new IllegalArgumentException(
                    "Driver must be approved before going online");
        }

        driver.setOnline(online);

        if (!online) {
            driver.setAvailable(false);
        } else {
            driver.setAvailable(true);
        }

        driverRepository.save(driver);
    }

    // Set driver available or unavailable
    public void updateAvailability(Long id, boolean available) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Driver not found"));

        if (available && (!driver.isApproved() || !driver.isOnline())) {
            throw new IllegalArgumentException(
                    "Driver must be approved and online to be available");
        }

        driver.setAvailable(available);
        driverRepository.save(driver);
    }

    // Update all driver statuses together
    public void updateDriverStatus(
            Long id,
            boolean approved,
            boolean online,
            boolean available) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Driver not found"));

        driver.setApproved(approved);

        if (!approved) {
            driver.setOnline(false);
            driver.setAvailable(false);
        } else {
            driver.setOnline(online);
            driver.setAvailable(online && available);
        }

        driverRepository.save(driver);
    }

    // Update driver's GPS location
    public Driver updateDriverLocation(
            Long id,
            Double latitude,
            Double longitude) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Driver not found"));

        if (latitude == null || longitude == null
                || !Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {

            throw new IllegalArgumentException(
                    "Invalid GPS coordinates");
        }

        driver.setLatitude(latitude);
        driver.setLongitude(longitude);

        return driverRepository.save(driver);
    }
}

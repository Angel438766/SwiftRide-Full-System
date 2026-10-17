
package taxicab.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import taxicab.model.Booking;
import taxicab.model.Driver;
import taxicab.model.Trip;
import taxicab.repository.BookingRepository;
import taxicab.repository.DriverRepository;
import taxicab.repository.TripRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;

    public BookingService(
            BookingRepository bookingRepository,
            DriverRepository driverRepository,
            TripRepository tripRepository) {
        this.bookingRepository = bookingRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsByPassengerId(Long passengerId) {
        return bookingRepository.findByPassengerId(passengerId);
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    // Get a driver by ID for passenger live tracking
    public Optional<Driver> getDriverById(Long driverId) {
        return driverRepository.findById(driverId);
    }

    public List<Driver> getAvailableDrivers() {
        return driverRepository
                .findByApprovedTrueAndOnlineTrueAndAvailableTrue();
    }

    public Booking saveBookingForPassenger(
            Booking booking, Long passengerId) {
        booking.setPassengerId(passengerId);
        return saveBooking(booking);
    }

    public Booking saveBooking(Booking booking) {
        if (booking.getPickupLocation() == null
                || booking.getPickupLocation().isBlank()
                || booking.getDestination() == null
                || booking.getDestination().isBlank()) {
            throw new IllegalArgumentException(
                    "Pickup and destination are required.");
        }

        if (booking.getVehicleCategory() == null
                || !List.of("Tuk Tuk", "Mini", "Car", "Van", "Premium")
                .contains(booking.getVehicleCategory())) {
            throw new IllegalArgumentException(
                    "Please select a valid vehicle category.");
        }

        Double distance = booking.getDistanceKm();

        if (distance != null
                && (!Double.isFinite(distance)
                || distance <= 0 || distance > 1000)) {
            throw new IllegalArgumentException(
                    "Invalid route distance.");
        }

        booking.setStatus("PENDING");
        booking.setDriverId(null);
        booking.setDriverName(null);
        booking.setCancellationReason(null);

        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDateTime.now());
        }

        calculateFare(booking);

        return bookingRepository.save(booking);
    }

    private void calculateFare(Booking booking) {
        String category = booking.getVehicleCategory();

        double baseFare;
        double ratePerKm;

        switch (category) {
            case "Tuk Tuk" -> {
                baseFare = 300;
                ratePerKm = 50;
            }
            case "Mini" -> {
                baseFare = 500;
                ratePerKm = 70;
            }
            case "Car" -> {
                baseFare = 800;
                ratePerKm = 100;
            }
            case "Van" -> {
                baseFare = 1200;
                ratePerKm = 140;
            }
            case "Premium" -> {
                baseFare = 1500;
                ratePerKm = 180;
            }
            default -> throw new IllegalArgumentException(
                    "Unknown vehicle category.");
        }

        double distance = booking.getDistanceKm() == null
                ? 0 : booking.getDistanceKm();

        booking.setFare(Math.round(baseFare + distance * ratePerKm));
    }

    public boolean assignDriver(Long bookingId, Long driverId) {
        Optional<Booking> bookingOptional =
                bookingRepository.findById(bookingId);
        Optional<Driver> driverOptional =
                driverRepository.findById(driverId);

        if (bookingOptional.isEmpty() || driverOptional.isEmpty()) {
            return false;
        }

        Booking booking = bookingOptional.get();
        Driver driver = driverOptional.get();

        if ("COMPLETED".equals(booking.getStatus())
                || "CANCELLED".equals(booking.getStatus())) {
            return false;
        }

        if (!driver.isApproved()
                || !driver.isOnline()
                || !driver.isAvailable()) {
            return false;
        }

        Long previousDriverId = booking.getDriverId();

        if ("STARTED".equals(booking.getStatus())
                && previousDriverId != null
                && !previousDriverId.equals(driverId)) {
            return false;
        }

        if (previousDriverId != null
                && !previousDriverId.equals(driverId)) {
            booking.setDriverId(null);
            booking.setDriverName(null);
            bookingRepository.save(booking);
            releaseDriver(previousDriverId, bookingId);
        }

        booking.setDriverId(driver.getId());
        booking.setDriverName(driver.getName());
        booking.setStatus("ACCEPTED");

        driver.setAvailable(false);

        bookingRepository.save(booking);
        driverRepository.save(driver);

        syncTrip(booking, "ACCEPTED");
        return true;
    }

    public boolean cancelBooking(Long id, String reason) {
        Optional<Booking> optional = bookingRepository.findById(id);

        if (optional.isEmpty()) {
            return false;
        }

        Booking booking = optional.get();

        if ("COMPLETED".equals(booking.getStatus())
                || "CANCELLED".equals(booking.getStatus())) {
            return false;
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Please provide a cancellation reason.");
        }

        booking.setCancellationReason(reason.trim());
        booking.setStatus("CANCELLED");

        bookingRepository.save(booking);
        syncTrip(booking, "CANCELLED");

        if (booking.getDriverId() != null) {
            releaseDriver(booking.getDriverId(), booking.getId());
        }

        return true;
    }

    public void updateStatus(Long id, String status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found."));

        if (status == null || !List.of(
                "PENDING", "ACCEPTED", "STARTED",
                "COMPLETED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Invalid booking status.");
        }

        if ("COMPLETED".equals(booking.getStatus())
                || "CANCELLED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "This booking is already finished.");
        }

        if ("ACCEPTED".equals(status)
                && booking.getDriverId() == null) {
            throw new IllegalArgumentException(
                    "Assign a driver before accepting the booking.");
        }

        if (("STARTED".equals(status) || "COMPLETED".equals(status))
                && booking.getDriverId() == null) {
            throw new IllegalArgumentException(
                    "Assign a driver before starting or completing the trip.");
        }

        if ("STARTED".equals(status)
                && !"ACCEPTED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only an accepted booking can be started.");
        }

        if ("COMPLETED".equals(status)
                && !"STARTED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Start the trip before completing it.");
        }

        if ("CANCELLED".equals(status)
                && (booking.getCancellationReason() == null
                || booking.getCancellationReason().isBlank())) {
            booking.setCancellationReason("Cancelled by administrator");
        }

        booking.setStatus(status);
        bookingRepository.save(booking);
        syncTrip(booking, status);

        if ("STARTED".equals(status) && booking.getDriverId() != null) {
            driverRepository.findById(booking.getDriverId())
                    .ifPresent(driver -> {
                        driver.setAvailable(false);
                        driverRepository.save(driver);
                    });
        }

        if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
            if (booking.getDriverId() != null) {
                releaseDriver(booking.getDriverId(), booking.getId());
            }
        }
    }

    private void releaseDriver(Long driverId, Long excludedBookingId) {
        driverRepository.findById(driverId).ifPresent(driver -> {
            boolean hasAnotherActiveBooking = bookingRepository.findAll()
                    .stream()
                    .anyMatch(booking ->
                            driverId.equals(booking.getDriverId())
                                    && !booking.getId().equals(excludedBookingId)
                                    && !List.of("COMPLETED", "CANCELLED")
                                    .contains(booking.getStatus()));

            driver.setAvailable(!hasAnotherActiveBooking
                    && driver.isApproved() && driver.isOnline());

            driverRepository.save(driver);
        });
    }

    private void syncTrip(Booking booking, String bookingStatus) {
        if ("PENDING".equals(bookingStatus)) {
            return;
        }

        Trip trip = tripRepository.findByBookingId(booking.getId())
                .orElseGet(Trip::new);

        trip.setBookingId(booking.getId());
        trip.setDriverName(booking.getDriverName());
        trip.setPickupLocation(booking.getPickupLocation());
        trip.setDestination(booking.getDestination());

        switch (bookingStatus) {
            case "ACCEPTED" -> {
                if (trip.getStatus() == null
                        || "NOT_STARTED".equals(trip.getStatus())) {
                    trip.setStatus("NOT_STARTED");
                }
            }
            case "STARTED" -> {
                if (trip.getStartTime() == null) {
                    trip.setStartTime(LocalDateTime.now());
                }
                trip.setStatus("STARTED");
            }
            case "COMPLETED" -> {
                if (trip.getStartTime() == null) {
                    trip.setStartTime(LocalDateTime.now());
                }
                if (trip.getEndTime() == null) {
                    trip.setEndTime(LocalDateTime.now());
                }
                trip.setStatus("COMPLETED");
            }
            case "CANCELLED" -> {
                if ("STARTED".equals(trip.getStatus())
                        && trip.getEndTime() == null) {
                    trip.setEndTime(LocalDateTime.now());
                }
                trip.setStatus("CANCELLED");
            }
            default -> {
                return;
            }
        }

        tripRepository.save(trip);
    }

    public void deleteBooking(Long id) {
        bookingRepository.findById(id).ifPresent(booking -> {
            Long driverId = booking.getDriverId();

            tripRepository.findByBookingId(id)
                    .ifPresent(tripRepository::delete);

            bookingRepository.delete(booking);

            if (driverId != null) {
                releaseDriver(driverId, id);
            }
        });
    }
}

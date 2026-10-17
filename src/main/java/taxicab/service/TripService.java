package taxicab.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import taxicab.model.Trip;
import taxicab.model.Booking;
import taxicab.repository.TripRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TripService {

    private final TripRepository tripRepository;
    private final BookingService bookingService;

    public TripService(
            TripRepository tripRepository,
            BookingService bookingService) {
        this.tripRepository = tripRepository;
        this.bookingService = bookingService;
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findById(id);
    }

    public Trip saveTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    // Automatically create a Trip for an accepted Booking
    public Trip createTripFromAcceptedBooking(Booking booking) {

        if (booking == null || booking.getId() == null) {
            throw new IllegalArgumentException(
                    "Valid booking is required.");
        }

        if (!"ACCEPTED".equals(booking.getStatus())) {
            throw new IllegalStateException(
                    "Only accepted bookings can create trips.");
        }

        // Prevent duplicate trips for the same booking
        Optional<Trip> existingTrip = tripRepository.findAll()
                .stream()
                .filter(trip ->
                        booking.getId().equals(trip.getBookingId()))
                .findFirst();

        if (existingTrip.isPresent()) {
            return existingTrip.get();
        }

        Trip trip = new Trip();

        trip.setBookingId(booking.getId());
        trip.setPickupLocation(booking.getPickupLocation());
        trip.setDestination(booking.getDestination());
        trip.setStatus("NOT_STARTED");

        return tripRepository.save(trip);
    }

    public void startTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Trip not found: " + id));

        if (!"NOT_STARTED".equals(trip.getStatus())) {
            throw new IllegalStateException(
                    "Only trips that have not started can be started.");
        }

        if (trip.getBookingId() == null) {
            throw new IllegalStateException(
                    "This trip has no linked booking.");
        }

        trip.setStartTime(LocalDateTime.now());
        trip.setStatus("STARTED");

        tripRepository.save(trip);

        bookingService.updateStatus(
                trip.getBookingId(), "STARTED");
    }

    public void completeTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Trip not found: " + id));

        if (!"STARTED".equals(trip.getStatus())) {
            throw new IllegalStateException(
                    "Only started trips can be completed.");
        }

        if (trip.getBookingId() == null) {
            throw new IllegalStateException(
                    "This trip has no linked booking.");
        }

        trip.setEndTime(LocalDateTime.now());
        trip.setStatus("COMPLETED");

        tripRepository.save(trip);

        bookingService.updateStatus(
                trip.getBookingId(), "COMPLETED");
    }

    public void deleteTrip(Long id) {
        tripRepository.deleteById(id);
    }
}

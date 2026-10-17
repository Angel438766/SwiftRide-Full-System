package taxicab.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import taxicab.model.Booking;
import taxicab.model.Cancellation;
import taxicab.model.Driver;
import taxicab.repository.BookingRepository;
import taxicab.repository.CancellationRepository;
import taxicab.repository.DriverRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CancellationService {

    private final CancellationRepository cancellationRepository;
    private final BookingRepository bookingRepository;
    private final DriverRepository driverRepository;

    public CancellationService(
            CancellationRepository cancellationRepository,
            BookingRepository bookingRepository,
            DriverRepository driverRepository) {
        this.cancellationRepository = cancellationRepository;
        this.bookingRepository = bookingRepository;
        this.driverRepository = driverRepository;
    }

    public List<Cancellation> getAllCancellations() {
        return cancellationRepository.findAll();
    }

    @Transactional
    public Cancellation cancelBooking(Long bookingId, String reason) {

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Cancellation reason is required.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found: " + bookingId));

        if ("COMPLETED".equalsIgnoreCase(booking.getStatus())
                || "CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException(
                    "This booking cannot be cancelled.");
        }

        // Release the assigned driver, if there is one.
        Long driverId = booking.getDriverId();

        if (driverId != null) {
            Driver driver = driverRepository.findById(driverId)
                    .orElse(null);

            if (driver != null && driver.isOnline()
                    && driver.isApproved()) {
                driver.setAvailable(true);
                driverRepository.save(driver);
            }
        }

        // Update booking status and cancellation reason.
        booking.setStatus("CANCELLED");
        booking.setCancellationReason(reason);
        bookingRepository.save(booking);

        // Save cancellation record.
        Cancellation cancellation = new Cancellation();
        cancellation.setBookingId(bookingId);
        cancellation.setReason(reason);
        cancellation.setCancellationTime(LocalDateTime.now());

        return cancellationRepository.save(cancellation);
    }

    public void deleteCancellation(Long id) {
        cancellationRepository.deleteById(id);
    }
}


package taxicab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxicab.model.Trip;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByBookingId(Long bookingId);
}

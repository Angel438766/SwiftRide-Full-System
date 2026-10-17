
package taxicab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxicab.model.Booking;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByPassengerId(Long passengerId);

    List<Booking> findByDriverId(Long driverId);

    List<Booking> findByStatusOrderByBookingDateDesc(String status);
}

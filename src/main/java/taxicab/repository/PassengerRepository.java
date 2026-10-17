
package taxicab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxicab.model.Passenger;

import java.util.Optional;

public interface PassengerRepository
        extends JpaRepository<Passenger, Long> {

    Optional<Passenger> findByEmail(String email);

    boolean existsByEmail(String email);
}


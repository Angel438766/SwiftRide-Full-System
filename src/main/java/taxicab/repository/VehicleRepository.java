package taxicab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxicab.model.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}

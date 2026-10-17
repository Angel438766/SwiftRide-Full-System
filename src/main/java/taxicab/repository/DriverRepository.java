package taxicab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxicab.model.Driver;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByApprovedTrueAndOnlineTrueAndAvailableTrue();
}


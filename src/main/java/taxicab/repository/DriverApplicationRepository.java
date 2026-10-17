package taxicab.repository;

import taxicab.model.DriverApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface DriverApplicationRepository
        extends JpaRepository<DriverApplication, Long> {

    Optional<DriverApplication> findByEmailIgnoreCase(String email);

    List<DriverApplication> findAllByOrderByIdDesc();
}
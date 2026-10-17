package taxicab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxicab.model.Cancellation;

public interface CancellationRepository
        extends JpaRepository<Cancellation, Long> {

}

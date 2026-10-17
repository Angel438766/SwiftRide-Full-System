
package taxicab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxicab.model.ContactMessage;

public interface ContactMessageRepository
        extends JpaRepository<ContactMessage, Long> {
}

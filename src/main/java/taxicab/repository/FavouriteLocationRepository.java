package taxicab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxicab.model.FavouriteLocation;

import java.util.List;

public interface FavouriteLocationRepository
        extends JpaRepository<FavouriteLocation, Long> {

    List<FavouriteLocation> findByPassengerId(Long passengerId);
}

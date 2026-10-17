package taxicab.service;

import org.springframework.stereotype.Service;

import taxicab.model.FavouriteLocation;
import taxicab.model.Passenger;
import taxicab.repository.FavouriteLocationRepository;
import taxicab.repository.PassengerRepository;

import java.util.List;
import java.util.Optional;

@Service
public class FavouriteLocationService {

    private final FavouriteLocationRepository repository;
    private final PassengerRepository passengerRepository;

    public FavouriteLocationService(
            FavouriteLocationRepository repository,
            PassengerRepository passengerRepository) {
        this.repository = repository;
        this.passengerRepository = passengerRepository;
    }

    // Get favourite locations belonging to a passenger
    public List<FavouriteLocation> getLocationsForPassenger(
            Long passengerId) {
        return repository.findByPassengerId(passengerId);
    }

    // Get a favourite location only if it belongs to that passenger
    public Optional<FavouriteLocation> getLocationForPassenger(
            Long id, Long passengerId) {

        return repository.findById(id)
                .filter(location ->
                        location.getPassenger() != null
                                && passengerId.equals(
                                location.getPassenger().getId()));
    }

    // Save a favourite location for a passenger
    public FavouriteLocation saveLocation(
            Long passengerId, FavouriteLocation location) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Passenger not found"));

        location.setId(null);
        location.setPassenger(passenger);

        return repository.save(location);
    }

    // Update a passenger's own favourite location
    public FavouriteLocation updateLocation(
            Long id,
            Long passengerId,
            FavouriteLocation updatedLocation) {

        FavouriteLocation location =
                getLocationForPassenger(id, passengerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Favourite location not found"));

        location.setLocationName(updatedLocation.getLocationName());
        location.setAddress(updatedLocation.getAddress());

        return repository.save(location);
    }

    // Delete a passenger's own favourite location
    public void deleteLocation(Long id, Long passengerId) {

        FavouriteLocation location =
                getLocationForPassenger(id, passengerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Favourite location not found"));

        repository.delete(location);
    }
}

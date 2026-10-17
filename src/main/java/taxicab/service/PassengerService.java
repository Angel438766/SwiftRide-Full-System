
package taxicab.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import taxicab.model.Passenger;
import taxicab.repository.PassengerRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PassengerService {

    private final PassengerRepository passengerRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public PassengerService(PassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

    public List<Passenger> getAllPassengers() {
        return passengerRepository.findAll();
    }

    public Optional<Passenger> getPassengerById(Long id) {
        return passengerRepository.findById(id);
    }

    public Passenger registerPassenger(Passenger passenger) {
        if (passengerRepository.existsByEmail(passenger.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        passenger.setPassword(
                passwordEncoder.encode(passenger.getPassword()));
        passenger.setActive(true);

        return passengerRepository.save(passenger);
    }

    public Optional<Passenger> loginPassenger(
            String email, String password) {

        Optional<Passenger> result =
                passengerRepository.findByEmail(email);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        Passenger passenger = result.get();

        if (!passenger.isActive()) {
            return Optional.empty();
        }

        String storedPassword = passenger.getPassword();

        if (storedPassword == null || password == null) {
            return Optional.empty();
        }

        if (isBCryptPassword(storedPassword)) {
            if (passwordEncoder.matches(password, storedPassword)) {
                return Optional.of(passenger);
            }

            return Optional.empty();
        }

        // Upgrade an older plain-text password after successful login.
        if (storedPassword.equals(password)) {
            passenger.setPassword(passwordEncoder.encode(password));
            passengerRepository.save(passenger);
            return Optional.of(passenger);
        }

        return Optional.empty();
    }

    public Passenger updatePassenger(
            Long id, Passenger updatedPassenger) {

        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Passenger not found"));

        passenger.setName(updatedPassenger.getName());
        passenger.setPhone(updatedPassenger.getPhone());
        passenger.setFavouriteLocation(
                updatedPassenger.getFavouriteLocation());

        return passengerRepository.save(passenger);
    }

    public void changePassword(
            Long id,
            String currentPassword,
            String newPassword,
            String confirmPassword) {

        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Passenger not found"));

        String storedPassword = passenger.getPassword();

        if (storedPassword == null || currentPassword == null
                || !passwordMatches(currentPassword, storedPassword)) {
            throw new IllegalArgumentException(
                    "Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException(
                    "New password must contain at least 6 characters");
        }

        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException(
                    "New password must be different from current password");
        }

        if (confirmPassword == null
                || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException(
                    "New password and confirmation do not match");
        }

        passenger.setPassword(passwordEncoder.encode(newPassword));
        passengerRepository.save(passenger);
    }

    public void deactivatePassenger(Long id) {
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Passenger not found"));

        passenger.setActive(false);
        passengerRepository.save(passenger);
    }

    private boolean passwordMatches(
            String rawPassword, String storedPassword) {

        if (isBCryptPassword(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        return storedPassword.equals(rawPassword);
    }

    private boolean isBCryptPassword(String password) {
        return password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }

    public void deletePassenger(Long id) {
        passengerRepository.deleteById(id);
    }
}

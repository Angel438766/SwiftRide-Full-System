package taxicab.service;

import taxicab.model.User;
import taxicab.repository.UserRepository;

import java.util.Optional;

public class AuthenticationService {

    private final UserRepository userRepository;

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean login(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            return user.get().getPassword().equals(password);
        }

        return false;
    }
}

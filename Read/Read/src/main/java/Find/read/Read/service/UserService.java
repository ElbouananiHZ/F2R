package Find.read.Read.service;

import Find.read.Read.models.User;
import Find.read.Read.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserService {
    @Autowired


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public boolean checkPassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPassword());
    }
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }



    public void saveUser(User user) {
        userRepository.save(user);  // Save user to the database
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);  // Encrypt password before saving
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);  // Validate password
    }

    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);  // This queries the database
    }

    public User findUserById(String id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElse(null); // Returns null if user not found
    }

    public void updateUser(User user) {
        // Get the existing user from DB to preserve some fields if needed
        User existingUser = userRepository.findById(user.getId()).orElse(null);

        if (existingUser != null) {
            // Preserve the password if it's not being updated
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                user.setPassword(existingUser.getPassword());
            }

            // Preserve the role if it's not being updated
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole(existingUser.getRole());
            }

            // Preserve registration date
            user.setRegistrationDate(existingUser.getRegistrationDate());
        }

        userRepository.save(user);
    }
    // Add these methods to UserService.java

    public boolean addNovelToLibrary(String userId, String novelId) {
        User user = findUserById(userId);
        if (user != null) {
            user.addNovelToLibrary(novelId);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean removeNovelFromLibrary(String userId, String novelId) {
        User user = findUserById(userId);
        if (user != null) {
            user.removeNovelFromLibrary(novelId);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public Set<String> getUserLibrary(String userId) {
        User user = findUserById(userId);
        return user != null ? user.getMyLibrary() : new HashSet<>();
    }
    // In UserService.java
    @Transactional
    public void trackNovelView(String userId, String novelId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.addViewedNovel(novelId);
        userRepository.save(user);
    }

    // In UserService.java



    // In UserService.java - Update the getUserHistory method
    public List<String> getUserHistory(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getViewedNovels() == null) {
            return Collections.emptyList();
        }


        List<String> history = new ArrayList<>(user.getViewedNovels());
        Collections.reverse(history); // This reverses the list in-place
        return history;


    }
    public User findById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }
    public void markNovelComplete(String userId, String novelId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Ajouter le novelId dans la collection des romans terminés
        user.getCompletedNovels().add(novelId);

        // Sauvegarder les modifications
        userRepository.save(user);
    }


}





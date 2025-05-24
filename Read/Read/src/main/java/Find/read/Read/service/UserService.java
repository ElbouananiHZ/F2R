package Find.read.Read.service;

import Find.read.Read.enums.NovelTag;
import Find.read.Read.models.Novel;
import Find.read.Read.models.User;
import Find.read.Read.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    public void trackNovelView(String userId, String novelId , Novel novel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));


        if (!user.getViewedNovels().contains(novelId)) {
            user.addViewedNovel(novelId);

            // Update tag preferences (if novel has tags)
            if (novel.getTags() != null) {
                for (NovelTag tag : novel.getTags()) {
                    user.updateTagPreference(tag, 1); // Increment tag counter
                }
            }

            // Update category preference (if novel has a category)
            if (novel.getCategory() != null) {
                user.updateCategoryPreference(novel.getCategory().name(), 1); // Increment category counter
            }

            userRepository.save(user); // Save changes
        }
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



    public void updateUserPreferences(String userId, String novelId, Novel novel) {
        User user = findUserById(userId);
        if (user == null) return;

        // Only update preferences if this is the first time viewing this novel
        if (!user.getViewedNovels().contains(novelId)) {
            // Update tag preferences
            for (NovelTag tag : novel.getTags()) {
                user.updateTagPreference(tag, 1);
            }

            // Update category preference
            if (novel.getCategory() != null) {
                user.updateCategoryPreference(novel.getCategory().name(), 1);
            }

            userRepository.save(user);
        }
    }

    public List<Novel> getRecommendedNovels(String userId, List<Novel> allNovels) {
        User user = findUserById(userId);
        if (user == null || user.getTagPreferences().isEmpty()) {
            return Collections.emptyList();
        }

        // Get user's top 3 tags
        List<NovelTag> topTags = user.getTagPreferences().entrySet().stream()
                .sorted(Map.Entry.<NovelTag, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Get user's top category
        String topCategory = user.getCategoryPreferences().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // Filter novels that match user's preferences and haven't been viewed
        Set<String> viewedNovels = user.getViewedNovels();
        List<Novel> recommended = allNovels.stream()
                .filter(novel -> !viewedNovels.contains(novel.getId()))
                .filter(novel -> {
                    // Score based on matching tags and category
                    int score = 0;

                    // Check tags
                    if (novel.getTags() != null) {
                        for (NovelTag tag : novel.getTags()) {
                            if (topTags.contains(tag)) {
                                score += user.getTagPreferences().getOrDefault(tag, 0);
                            }
                        }
                    }

                    // Check category
                    if (topCategory != null && novel.getCategory() != null
                            && novel.getCategory().name().equals(topCategory)) {
                        score += user.getCategoryPreferences().getOrDefault(topCategory, 0);
                    }

                    return score > 0;
                })
                .sorted((n1, n2) -> {
                    // Compare by score (descending)
                    int score1 = calculateNovelScore(n1, user, topTags, topCategory);
                    int score2 = calculateNovelScore(n2, user, topTags, topCategory);
                    return Integer.compare(score2, score1);
                })
                .limit(3) // Return top 3 recommendations
                .collect(Collectors.toList());

        return recommended;
    }

    private int calculateNovelScore(Novel novel, User user, List<NovelTag> topTags, String topCategory) {
        int score = 0;

        // Add points for matching tags
        if (novel.getTags() != null) {
            for (NovelTag tag : novel.getTags()) {
                if (topTags.contains(tag)) {
                    score += user.getTagPreferences().getOrDefault(tag, 0);
                }
            }
        }

        // Add points for matching category
        if (topCategory != null && novel.getCategory() != null
                && novel.getCategory().name().equals(topCategory)) {
            score += user.getCategoryPreferences().getOrDefault(topCategory, 0);
        }

        return score;
    }

}





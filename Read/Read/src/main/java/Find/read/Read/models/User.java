package Find.read.Read.models;

import Find.read.Read.enums.NovelTag;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "users")
public class User {

    @Id
    private String id;
    private String username;
    private String email;
    private String password;
    private String role; // "ADMIN", "AUTHOR", or "VISITOR"
    private List<Comment> comments;

    // Constructors
    private LocalDate lastUsernameChangeDate;
    private LocalDate registrationDate;
    private byte[] profilePicture;
    private Set<String> myLibrary = new HashSet<>();

    private Set<String> viewedNovels = new LinkedHashSet<>();// Maintains insertion order
    private LocalDateTime lastViewedDate;
    private Map<NovelTag, Integer> tagPreferences = new HashMap<>();


    private Set<String> ratedNovels = new HashSet<>();

    private Map<String, Integer> categoryPreferences = new HashMap<>();





    public Map<NovelTag, Integer> getTagPreferences() {
        return tagPreferences;
    }

    public void setTagPreferences(Map<NovelTag, Integer> tagPreferences) {
        this.tagPreferences = tagPreferences;
    }

    public void setCategoryPreferences(Map<String, Integer> categoryPreferences) {
        this.categoryPreferences = categoryPreferences;
    }



    public Map<String, Integer> getCategoryPreferences() {
        return categoryPreferences;
    }

    public void updateCategoryPreference(String category, int increment) {
        categoryPreferences.merge(category, increment, Integer::sum);
    }


    public Set<String> getRatedNovels() {
        return ratedNovels;
    }

    public void setRatedNovels(Set<String> ratedNovels) {
        this.ratedNovels = ratedNovels;
    }



    public void updateTagPreference(NovelTag tag, int increment) {
        tagPreferences.merge(tag, increment, Integer::sum);
    }
    // Getters and Setters
    public Set<String> getViewedNovels() {
        return viewedNovels;
    }

    public void setViewedNovels(Set<String> viewedNovels) {
        this.viewedNovels = viewedNovels;
    }

    public LocalDateTime getLastViewedDate() {
        return lastViewedDate;
    }

    public void setLastViewedDate(LocalDateTime lastViewedDate) {
        this.lastViewedDate = lastViewedDate;
    }

    public void addViewedNovel(String novelId) {
        this.viewedNovels.add(novelId);
        this.lastViewedDate = LocalDateTime.now();
    }



    public Set<String> getMyLibrary() {
        return myLibrary;
    }

    public void setMyLibrary(Set<String> myLibrary) {
        this.myLibrary = myLibrary;
    }

    public void addNovelToLibrary(String novelId) {
        this.myLibrary.add(novelId);
    }

    public void removeNovelFromLibrary(String novelId) {
        this.myLibrary.remove(novelId);
    }
    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public LocalDate getLastUsernameChangeDate() {
        return lastUsernameChangeDate;
    }

    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }
    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }


    public void setLastUsernameChangeDate(LocalDate lastUsernameChangeDate) {
        this.lastUsernameChangeDate = lastUsernameChangeDate;
    }




    // Constructors, getters, and setters
    public User() {
    }

    public User(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }
    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

package Find.read.Read.models;

import Find.read.Read.enums.NovelCategory;
import Find.read.Read.enums.NovelTag;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Document(collection = "novels")
@CompoundIndex(def = "{'name': 'text', 'category': 'text', 'description': 'text'}")
@CompoundIndexes({
        @CompoundIndex(name = "tags_rating_idx", def = "{'tags': 1, 'averageRating': -1}"),
        @CompoundIndex(name = "rating_idx", def = "{'averageRating': -1}")
})
public class Novel {





    @Id
    private String id;
    @DBRef(lazy = true)
    private List<Page> pages = new ArrayList<>();
    private String authorId;
    private byte[] imageData;  // Image data for internal storage or retrieval
    private String name;
    private String summary;
    private String pic;
    private NovelCategory category;
    private Set<NovelTag> tags;
    private String imageName;  // Store the name of the file here
    private int totalRating = 0;
    private int ratingCount = 0;
    private Double rating;
    private double averageRating; // Field for average rating

    private String authorName;

    public String getAuthorName() {
        return authorName;
    }
    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public Double getRating() {
        return rating;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }


    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }




    public double getAverageRatingRounded() {
        return Math.round(getAverageRating() * 10.0) / 10.0;
    }


    public void setRating(Double rating) {
        this.rating = rating;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public int getTotalRating() {
        return totalRating;
    }

    public void setTotalRating(int totalRating) {
        this.totalRating = totalRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }



    public String getImageName() {
        return imageName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public NovelCategory getCategory() {
        return category;
    }

    public void setCategory(NovelCategory category) {
        this.category = category;
    }

    public Set<NovelTag> getTags() {
        return tags;
    }

    public void setTags(Set<NovelTag> tags) {
        this.tags = tags;
    }

    public void setId(String id) {
        this.id = id;
    }
}

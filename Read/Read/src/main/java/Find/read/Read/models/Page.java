package Find.read.Read.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Document(collection = "pages")
public class Page {

    @Id
    private String id;
    private String name; // title of the page/chapter
    private int viewCount = 0;
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private int pageNumber;
    private String content;

    private String novelId; // Just storing the ID for fast queries
    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
    @DBRef
    private Novel novel; // Optional, for direct access if needed

    // Constructors
    public Page() {}

    public Page(String id, int pageNumber, String content, String novelId, Novel novel) {
        this.id = id;
        this.pageNumber = pageNumber;
        this.content = content;
        this.novelId = novelId;
        this.novel = novel;
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNovelId() {
        return novelId;
    }

    public void setNovelId(String novelId) {
        this.novelId = novelId;
    }

    public Novel getNovel() {
        return novel;
    }

    public void setNovel(Novel novel) {
        this.novel = novel;
    }
}

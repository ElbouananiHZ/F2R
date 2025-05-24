package Find.read.Read.service;

import Find.read.Read.models.Page;
import Find.read.Read.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PageService {

    private final PageRepository pageRepository;
    private final NovelService novelService; // Add this

    @Autowired
    public PageService(PageRepository pageRepository, NovelService novelService) {
        this.pageRepository = pageRepository;
        this.novelService = novelService; // Initialize
    }

    public Optional<Page> getPageByNovelIdAndPageNumber(String novelId, int pageNumber) {
        return pageRepository.findByNovelIdAndPageNumber(novelId, pageNumber);
    }

    public void savePage(Page page) {
        pageRepository.save(page);
    }

    @Transactional
    public void deletePageAndRenumber(String novelId, int pageNumber) {
        System.out.println("Attempting to delete page " + pageNumber + " from novel " + novelId);

        // 1. First remove from Novel's reference list
        novelService.removePageFromNovel(novelId, pageNumber);

        // 2. Then delete from Page collection
        Optional<Page> pageToDelete = pageRepository.findByNovelIdAndPageNumber(novelId, pageNumber);
        if (!pageToDelete.isPresent()) {
            System.out.println("Page not found for deletion");
            return;
        }

        pageRepository.delete(pageToDelete.get());
        System.out.println("Page deleted from pages collection");

        // 3. Renumber remaining pages
        List<Page> pages = pageRepository.findByNovelId(novelId);
        System.out.println("Found " + pages.size() + " remaining pages to renumber");

        for (Page page : pages) {
            if (page.getPageNumber() > pageNumber) {
                System.out.println("Renumbering page " + page.getPageNumber() + " to " + (page.getPageNumber() - 1));
                page.setPageNumber(page.getPageNumber() - 1);
                pageRepository.save(page);
            }
        }
    }

    public void updatePageContent(String novelId, int pageNumber, String content) {
        Optional<Page> page = pageRepository.findByNovelIdAndPageNumber(novelId, pageNumber);
        page.ifPresent(p -> {
            p.setContent(content);
            pageRepository.save(p);
        });
    }
    public void incrementPageView(String novelId, int pageNumber) {
        Page page = getPageByNovelIdAndPageNumber(novelId, pageNumber)
                .orElseThrow(() -> new RuntimeException("Page not found"));
        page.setViewCount(page.getViewCount() + 1);
        // Assuming you have a pageRepository.save() method
        pageRepository.save(page);
    }
}
package Find.read.Read.service;

import Find.read.Read.controller.NovelController;
import Find.read.Read.enums.NovelCategory;
import Find.read.Read.enums.NovelTag;
import Find.read.Read.models.Novel;
import Find.read.Read.models.Page;
import Find.read.Read.models.Rating;
import Find.read.Read.models.User;
import Find.read.Read.repository.NovelRepository;
import Find.read.Read.repository.PageRepository;
import Find.read.Read.repository.RatingRepository;
import Find.read.Read.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NovelService {
    private static final Logger logger = LoggerFactory.getLogger(NovelController.class);
    private final NovelRepository novelRepository;
    private final PageRepository pageRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final MongoOperations mongoOperations;

    @Autowired
    public NovelService(NovelRepository novelRepository,
                        PageRepository pageRepository,
                        RatingRepository ratingRepository,
                        UserRepository userRepository,
                        MongoOperations mongoOperations) {
        this.novelRepository = novelRepository;
        this.pageRepository = pageRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.mongoOperations = mongoOperations;
    }

    // Basic CRUD operations
    public Optional<Novel> getNovelById(String id) {
        return novelRepository.findById(id);
    }

    public List<Novel> getAllNovels() {
        return novelRepository.findAll();
    }

    public Novel saveNovel(Novel novel) {
        return novelRepository.save(novel);
    }

    public void deleteNovel(String id) {
        novelRepository.deleteById(id);
    }

    // Page operations
    public Page savePage(Page page) {
        return pageRepository.save(page);
    }

    public void updatePageContent(String novelId, int pageNumber, String content) {
        Optional<Page> page = pageRepository.findByNovelIdAndPageNumber(novelId, pageNumber);
        page.ifPresent(p -> {
            p.setContent(content);
            pageRepository.save(p);
        });
    }

    @Transactional
    public void deletePageAndRenumber(String novelId, int pageNumber) {
        System.out.println("Attempting to delete page " + pageNumber + " from novel " + novelId);

        Optional<Page> pageToDelete = pageRepository.findByNovelIdAndPageNumber(novelId, pageNumber);
        if (pageToDelete.isPresent()) {
            System.out.println("Page found, deleting...");
            pageRepository.delete(pageToDelete.get());

            List<Page> pages = pageRepository.findByNovelId(novelId);
            System.out.println("Found " + pages.size() + " remaining pages to renumber");

            for (Page page : pages) {
                if (page.getPageNumber() > pageNumber) {
                    System.out.println("Renumbering page " + page.getPageNumber() + " to " + (page.getPageNumber() - 1));
                    page.setPageNumber(page.getPageNumber() - 1);
                    pageRepository.save(page);
                }
            }
        } else {
            System.out.println("Page not found for deletion");
        }
    }

    @Transactional
    public void removePageFromNovel(String novelId, int pageNumber) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        System.out.println("Before removal: " + novel.getPages().size() + " pages");
        novel.getPages().removeIf(p -> p.getPageNumber() == pageNumber);
        System.out.println("After removal: " + novel.getPages().size() + " pages");

        novelRepository.save(novel);
    }

    // Rating operations
    public void updateAverageRating(String novelId) {
        List<Rating> ratings = ratingRepository.findByNovelId(novelId);
        double avg = ratings.stream()
                .mapToInt(Rating::getRating)
                .average()
                .orElse(0.0);

        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        novel.setAverageRating(avg);
        novel.setRatingCount(ratings.size());
        novel.setTotalRating(ratings.stream().mapToInt(Rating::getRating).sum());

        novelRepository.save(novel);
    }

    // Novel retrieval operations
    public List<Novel> getNovelsByIds(List<String> novelIds) {
        return novelRepository.findAllById(novelIds);
    }



    public List<Novel> getNovelsOrderedByRating() {
        List<Novel> allNovels = novelRepository.findAll();

        System.out.println("Total novels fetched: " + allNovels.size());
        allNovels.forEach(novel -> System.out.println(
                "Novel: " + novel.getName() +
                        ", Rating: " + novel.getAverageRating() +
                        ", Count: " + novel.getRatingCount()
        ));

        return allNovels.stream()
                .sorted(Comparator.comparing(
                        Novel::getAverageRating,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());
    }

    // Search operations
    public List<Novel> searchNovels(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Use the comprehensive text search with proper scoring
        TextCriteria criteria = TextCriteria.forDefaultLanguage()
                .matchingAny(query.split(" "))
                .caseSensitive(false);

        Query mongoQuery = Query.query(criteria)
                .limit(50)
                .with(Sort.by(Sort.Direction.DESC, "score"));

        return mongoOperations.find(mongoQuery, Novel.class);
    }


    // In NovelService.java
    public List<Novel> getNovelsByAuthorId(String authorId) {
        logger.info("Searching for novels by authorId: {}", authorId);
        List<Novel> novels = novelRepository.findByAuthorId(authorId);
        logger.info("Found novels: {}", novels);
        return novels;
    }
    public void processNovelView(User user, Novel novel) {
        user.addViewedNovel(novel.getId());

        // Update category (string key)
        user.updateCategoryPreference(novel.getCategory().name(), 1);

        // Update tags (using NovelTag enum directly)
        for (NovelTag tag : novel.getTags()) {
            user.updateTagPreference(tag, 1);
        }

        userRepository.save(user);
    }

    public List<Novel> getRecommendationsForUser(User user) {
        Map<NovelTag, Integer> tagPrefs = user.getTagPreferences();
        Map<String, Integer> categoryPrefs = user.getCategoryPreferences();

        List<Novel> allNovels = novelRepository.findAll(); // or optimize with filtering
        Map<Novel, Integer> scoreMap = new HashMap<>();

        for (Novel novel : allNovels) {
            int score = 0;

            // Score by category (string keys)
            score += categoryPrefs.getOrDefault(novel.getCategory().name(), 0);

            // Score by tags (using NovelTag enum keys)
            for (NovelTag tag : novel.getTags()) {
                score += tagPrefs.getOrDefault(tag, 0);
            }

            scoreMap.put(novel, score);
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Novel, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .filter(novel -> !user.getViewedNovels().contains(novel.getId()))
                .limit(3)
                .toList();
    }


}
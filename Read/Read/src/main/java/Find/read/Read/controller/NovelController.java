package Find.read.Read.controller;

import Find.read.Read.enums.NovelCategory;
import Find.read.Read.enums.NovelTag;
import Find.read.Read.models.Comment;
import Find.read.Read.models.Novel;
import Find.read.Read.models.Rating;
import Find.read.Read.repository.CommentRepository;
import Find.read.Read.repository.RatingRepository;
import Find.read.Read.service.CommentService;
import Find.read.Read.service.NovelService;
import Find.read.Read.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/novels")
public class NovelController {
    @Autowired
    private UserService userService;  // Add this with your other autowired dependencies
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private RatingRepository ratingRepository;

    private final NovelService novelService;

    @Autowired
    public NovelController( NovelService novelService ) {

        this.novelService = novelService;

    }
    private static final Logger logger = LoggerFactory.getLogger(NovelController.class);
    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("userId") != null;
    }

    private boolean isOwner(HttpSession session, String novelId) {
        if (!isLoggedIn(session)) {
            return false;
        }
        String userId = (String) session.getAttribute("userId");
        return novelService.getNovelById(novelId)
                .map(novel -> novel.getAuthorId().equals(userId))
                .orElse(false);
    }

    @GetMapping("/unauthorized")
    public String unauthorizedPage() {
        return "unauthorized";
    }




    @GetMapping("/{id}")
    public String showNovel(@PathVariable String id, Model model) {
        Novel novel = novelService.getNovelById(id)
                .orElseThrow(() -> new RuntimeException("Novel not found"));
        model.addAttribute("novel", novel);
        return "novel/detail";
    }

    @GetMapping
    public String listNovels(Model model) {
        model.addAttribute("novels", novelService.getAllNovels());

        return "novel/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/auth/login";
        }

        model.addAttribute("novel", new Novel());
        model.addAttribute("categories", NovelCategory.values());
        model.addAttribute("tags", NovelTag.values());
        return "novel/create";
    }

    @PostMapping("/save")
    public String saveNovel(@ModelAttribute Novel novel,
                            @RequestParam("imageFile") MultipartFile imageFile,
                            HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/auth/login";
        }

        String userId = (String) session.getAttribute("userId");
        novel.setAuthorId(userId);
        novel.setId(UUID.randomUUID().toString());

        if (!imageFile.isEmpty()) {
            try {
                byte[] imageBytes = imageFile.getBytes();
                novel.setImageData(imageBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        novelService.saveNovel(novel);
        return "redirect:/novels/detail/" + novel.getId();
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, HttpSession session) {
        if (!isOwner(session, id)) {
            return "redirect:/novels/unauthorized"; // Redirect if not the owner
        }

        Novel novel = novelService.getNovelById(id)
                .orElseThrow(() -> new RuntimeException("Novel not found"));
        model.addAttribute("novel", novel);
        model.addAttribute("categories", NovelCategory.values());
        model.addAttribute("tags", NovelTag.values());
        return "novel/edit";
    }

    @PostMapping("/update/{id}")
    public String updateNovel(@PathVariable String id,
                              @ModelAttribute Novel updatedNovel,
                              @RequestParam("imageFile") MultipartFile imageFile,
                              HttpSession session) {
        if (!isOwner(session, id)) {
            return "redirect:/novels/unauthorized"; // Redirect if not the owner
        }

        Novel existingNovel = novelService.getNovelById(id)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        // Preserve author ID
        updatedNovel.setAuthorId(existingNovel.getAuthorId());
        updatedNovel.setId(id);

        if (!imageFile.isEmpty()) {
            try {
                updatedNovel.setImageData(imageFile.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            updatedNovel.setImageData(existingNovel.getImageData());
        }

        novelService.saveNovel(updatedNovel);
        return "redirect:/novels/detail/" + id;
    }

    @GetMapping("/detail/{id}")
    public String showNovelDetail(@PathVariable String id, Model model, HttpSession session) {
        try {
            Novel novel = novelService.getNovelById(id)
                    .orElseThrow(() -> new RuntimeException("Novel not found"));

            List<Comment> comments = commentRepository.findByNovelId(id);
            model.addAttribute("novel", novel);
            model.addAttribute("comments", comments);

            // Add this to the showNovelDetail method in NovelController.java
            if (isLoggedIn(session)) {


                String userId = (String) session.getAttribute("userId");
                userService.trackNovelView(userId, id);
                boolean isInLibrary = userService.getUserLibrary(userId).contains(id);
                model.addAttribute("isInLibrary", isInLibrary);
            }

            return "novel/detail";

        } catch (Exception e) {
            e.printStackTrace(); // PRINT the real error!
            model.addAttribute("error", e.getMessage());
            return "error"; // fallback error page
        }
    }


    @PostMapping("/delete/{id}")
    public String deleteNovel(@PathVariable String id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/auth/login";
        }

        Optional<Novel> novel = novelService.getNovelById(id);
        if (novel.isPresent()) {
            // Ensure the logged-in user is the owner of the novel
            if (isOwner(session, id)) {
                novelService.deleteNovel(id); // Perform the delete operation
                return "redirect:/novels"; // Redirect to the list of novels after deletion
            } else {
                return "redirect:/novels/unauthorized";
            }
        } else {
            throw new RuntimeException("Novel not found");
        }
    }





    @GetMapping("/image/{id}")
    @ResponseBody
    public byte[] serveImage(@PathVariable String id) {
        Novel novel = novelService.getNovelById(id)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        return novel.getImageData();
    }
    @Autowired
    private CommentService commentService;

    @PostMapping("/{novelId}/comments/{commentId}/reply")
    public String addReply(@PathVariable String novelId,
                           @PathVariable String commentId,
                           @RequestParam String replyContent,
                           HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/auth/login";

        }

        String userId = (String) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        commentService.addReply(commentId, userId, username, replyContent);

        return "redirect:/novels/detail/" + novelId;
    }

    @PostMapping("/{novelId}/comments/{commentId}/replies/{replyId}/delete")
    public String deleteReply(@PathVariable String novelId,
                              @PathVariable String commentId,
                              @PathVariable String replyId,
                              HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/auth/login";
        }

        String userId = (String) session.getAttribute("userId");
        commentService.deleteReply(commentId, replyId, userId);

        return "redirect:/novels/detail/" + novelId;
    }

    @PostMapping("/{id}/rate")
    public String rateNovel(@PathVariable String id, @RequestParam int rating, HttpSession session) {
        if (!isLoggedIn(session))   return "redirect:/auth/login";

        String userId = (String) session.getAttribute("userId");

        Optional<Rating> existingRating = ratingRepository.findByNovelIdAndUserId(id, userId);
        if (existingRating.isPresent()) {
            existingRating.get().setRating(rating);
            ratingRepository.save(existingRating.get());
        } else {
            Rating newRating = new Rating();
            newRating.setNovelId(id);
            newRating.setUserId(userId);
            newRating.setRating(rating);
            ratingRepository.save(newRating);
        }

        novelService.updateAverageRating(id); // implement this method in service

        return "redirect:/novels/detail/" + id;
    }
    @PostMapping("/{novelId}/comments")
    public String addComment(@PathVariable String novelId,
                             @RequestParam String commentContent,
                             HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/auth/login";
        }

        String userId = (String) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        Comment comment = new Comment();
        comment.setId(UUID.randomUUID().toString());
        comment.setNovelId(novelId);
        comment.setUserId(userId);
        comment.setUsername(username);
        comment.setContent(commentContent);

        commentRepository.save(comment);

        return "redirect:/novels/detail/" + novelId;
    }


    @GetMapping("/ranking")
    public String listNovelsByRank(Model model) {
        try {
            List<Novel> novels = novelService.getNovelsOrderedByRating();
            model.addAttribute("novels", novels);
            return "novel/Ranking";
        } catch (Exception e) {
            logger.error("Error loading ranking", e);
            model.addAttribute("error", "Could not load rankings");
            return "error";
        }
    }
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<List<Novel>> searchNovels(@RequestParam String query) {
        List<Novel> results = novelService.searchNovels(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search-page")
    public String searchPage(@RequestParam String query, Model model) {
        List<Novel> results = novelService.searchNovels(query);
        model.addAttribute("novels", results);
        model.addAttribute("searchQuery", query);
        return "novel/list"; // Make sure this matches your template name
    }
    @GetMapping("/my-novels")
    public String showUserNovels(Model model, HttpSession session) {
        if (!isLoggedIn(session)) {
            logger.error("User not logged in - redirecting to login");
            return "redirect:/auth/login";
        }

        String userId = (String) session.getAttribute("userId");
        logger.info("Fetching novels for user ID: {}", userId);

        List<Novel> userNovels = novelService.getNovelsByAuthorId(userId);
        logger.info("Found {} novels for user {}", userNovels.size(), userId);

        model.addAttribute("novels", userNovels);
        return "novel/my-novels";
    }





}

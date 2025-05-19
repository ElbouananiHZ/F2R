// HistoryController.java
package Find.read.Read.controller;

import Find.read.Read.models.Novel;
import Find.read.Read.models.User;
import Find.read.Read.service.NovelService;
import Find.read.Read.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class HistoryController {

    private final UserService userService;
    private final NovelService novelService;

    public HistoryController(UserService userService, NovelService novelService) {
        this.userService = userService;
        this.novelService = novelService;
    }

    @GetMapping("/history")
    public String showHistory(Model model, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        List<String> novelIds = userService.getUserHistory(userId);
        model.addAttribute("novels", novelService.getNovelsByIds(novelIds));

        return "history";
    }
    @GetMapping("/recommendation")
    public String getRecommendations(Model model, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        User user = userService.findById(userId);
        List<Novel> recommended = novelService.getRecommendationsForUser(user);

        // Fetch author names for each novel
        recommended.forEach(novel -> {
            User author = userService.findUserById(novel.getAuthorId());
            if (author != null) {
                novel.setAuthorName(author.getUsername());
            }
        });

        model.addAttribute("recommended", recommended);
        return "recommendations";
    }

    @GetMapping("/image/{id}")
    @ResponseBody
    public byte[] serveImage(@PathVariable String id) {
        Novel novel = novelService.getNovelById(id)
                .orElseThrow(() -> new RuntimeException("Novel not found"));
        return novel.getImageData();
    }

}
package Find.read.Read.controller;

import Find.read.Read.models.Novel;
import Find.read.Read.models.Page;
import Find.read.Read.models.User;
import Find.read.Read.service.NovelService;
import Find.read.Read.service.PageService;
import Find.read.Read.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/novels/{novelId}/pages")
public class PageController {

    private final NovelService novelService;
    private final PageService pageService;
    private final UserService userService;

    @Autowired
    public PageController(NovelService novelService, PageService pageService, UserService userService) {
        this.novelService = novelService;
        this.pageService = pageService;
        this.userService = userService;
    }

    // Utility method to get the currently logged-in user
    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();  // Assuming the username is the email
        return userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Check if the logged-in user is authorized to modify the novel
    private boolean isAuthorized(Novel novel) {
        User loggedInUser = getLoggedInUser();
        return novel.getAuthorId().equals(loggedInUser.getId());
    }

    @GetMapping("/{pageNumber}")
    public String viewPage(@PathVariable String novelId,
                           @PathVariable int pageNumber,
                           Model model) {
        Novel novel = novelService.getNovelById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        // Add the current logged-in user to the model
        try {
            User currentUser = getLoggedInUser();
            model.addAttribute("currentUser", currentUser);
        } catch (RuntimeException e) {
            // User not logged in or not found - that's fine, buttons won't show
        }

        pageService.incrementPageView(novelId, pageNumber);
        Page currentPage = novel.getPages().stream()
                .filter(p -> p.getPageNumber() == pageNumber)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Page not found"));

        model.addAttribute("novel", novel);
        model.addAttribute("page", currentPage);
        model.addAttribute("viewCount", currentPage.getViewCount());
        model.addAttribute("totalPages", novel.getPages().size());

        int nextPageNum = pageNumber + 1;
        int prevPageNum = pageNumber - 1;

        Optional<Page> nextPage = novel.getPages().stream()
                .filter(p -> p.getPageNumber() == nextPageNum)
                .findFirst();

        Optional<Page> previousPage = novel.getPages().stream()
                .filter(p -> p.getPageNumber() == prevPageNum)
                .findFirst();

        model.addAttribute("nextPage", nextPage.orElse(null));
        model.addAttribute("previousPage", previousPage.orElse(null));
        model.addAttribute("viewCount", currentPage.getViewCount());
        return "page-reader";
    }
    @GetMapping("/new")
    public String showAddPageForm(@PathVariable String novelId, Model model) {
        Novel novel = novelService.getNovelById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        if (!isAuthorized(novel)) {
            return "redirect:/novels/unauthorized";  // Redirect to unauthorized page
        }

        Page page = new Page();
        int nextPageNumber = (novel.getPages() != null) ? novel.getPages().size() + 1 : 1;
        page.setPageNumber(nextPageNumber);

        model.addAttribute("novelId", novelId);
        model.addAttribute("novelName", novel.getName());
        model.addAttribute("page", page);

        return "add-page";
    }

    @PostMapping("/save")
    public String addPage(@PathVariable String novelId, @ModelAttribute Page page) {
        Novel novel = novelService.getNovelById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        if (!isAuthorized(novel)) {
            return "redirect:/novels/unauthorized";  // Redirect to unauthorized page
        }

        page.setId(UUID.randomUUID().toString());
        page.setNovelId(novelId);
        page.setNovel(novel);

        if (novel.getPages() == null) {
            novel.setPages(new ArrayList<>());
        }

        novel.getPages().add(page);
        novelService.savePage(page);
        novelService.saveNovel(novel);

        return "redirect:/novels/detail/" + novelId;
    }

    @GetMapping("/{pageNumber}/edit")
    public String showEditPageForm(@PathVariable String novelId,
                                   @PathVariable int pageNumber,
                                   Model model) {
        Novel novel = novelService.getNovelById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        if (!isAuthorized(novel)) {
            return "redirect:/novels/unauthorized";  // Redirect to unauthorized page
        }

        Page page = pageService.getPageByNovelIdAndPageNumber(novelId, pageNumber)
                .orElseThrow(() -> new RuntimeException("Page not found"));

        model.addAttribute("novelId", novelId);
        model.addAttribute("novelName", novel.getName());
        model.addAttribute("page", page);

        return "edit-page";
    }

    @PostMapping("/{pageNumber}/edit")
    public String editPage(@PathVariable String novelId,
                           @PathVariable int pageNumber,
                           @ModelAttribute Page updatedPage) {
        Novel novel = novelService.getNovelById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        if (!isAuthorized(novel)) {
            return "redirect:/novels/unauthorized";  // Redirect to unauthorized page
        }

        pageService.updatePageContent(novelId, pageNumber, updatedPage.getContent());
        return "redirect:/novels/" + novelId + "/pages/" + pageNumber + "?success=page_updated";
    }

    @GetMapping("/{pageNumber}/delete")
    public String deletePage(@PathVariable String novelId,
                             @PathVariable int pageNumber) {
        Novel novel = novelService.getNovelById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        if (!isAuthorized(novel)) {
            return "redirect:/novels/unauthorized";  // Redirect to unauthorized page
        }

        pageService.deletePageAndRenumber(novelId, pageNumber);
        return "redirect:/novels/detail/" + novelId + "?success=page_deleted";
    }

    @GetMapping("/unauthorized")
    public String unauthorized() {
        return "unauthorized";  // The page that shows an unauthorized access message
    }
}

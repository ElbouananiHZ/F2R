package Find.read.Read.controller;



import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatPageController {

    @GetMapping("/rag-chat")
    public String showRagChatPage() {
        // Return the name of the Thymeleaf template (rag-chat.html)
        return "rag-chat";
    }
}

package Find.read.Read.controller;


import Find.read.Read.models.User;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("message", "Welcome to your Dashboard!");
        model.addAttribute("userProfileForm", new User());
        return "dashboard";
    }

    @PostMapping("/dashboard")
    public String updateProfile(@ModelAttribute("userProfileForm") User form,
                                BindingResult result,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("message", "Please correct the errors in the form.");
            return "dashboard";
        }

        // Simuler une sauvegarde (en vrai, appeler un service ici)
        model.addAttribute("message", "Profile updated successfully!");
        return "dashboard";
    }
}

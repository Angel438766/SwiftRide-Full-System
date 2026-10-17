package taxicab.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import taxicab.model.ContactMessage;
import taxicab.repository.ContactMessageRepository;

@Controller
public class PageController {

    private final ContactMessageRepository contactMessageRepository;

    public PageController(
            ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    // About Us page
    @GetMapping("/about")
    public String about() {
        return "about";
    }

    // Contact Us page
    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    // Save contact message
    @PostMapping("/contact")
    public String submitContact(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String message,
            Model model) {

        ContactMessage contactMessage = new ContactMessage();
        contactMessage.setName(name.trim());
        contactMessage.setEmail(email.trim());
        contactMessage.setMessage(message.trim());

        contactMessageRepository.save(contactMessage);

        model.addAttribute("submitted", true);
        model.addAttribute("name", name.trim());

        return "contact";
    }

    // Admin contact messages page
    @GetMapping("/admin/contact-messages")
    public String viewContactMessages(
            Model model,
            HttpSession session) {

        // Check whether the administrator is logged in
        if (!Boolean.TRUE.equals(
                session.getAttribute("swiftRideAdminLoggedIn"))) {
            return "redirect:/admin/login";
        }

        // Load all contact messages from the database
        model.addAttribute(
                "messages",
                contactMessageRepository.findAll()
        );

        return "contact-messages";
    }
}
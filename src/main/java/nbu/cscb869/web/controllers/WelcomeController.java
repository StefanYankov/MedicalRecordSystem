package nbu.cscb869.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to display the initial welcome/role selection page for new users.
 */
@Controller
public class WelcomeController {

    /**
     * Displays the welcome page where a new user can choose their path.
     *
     * @return The name of the welcome view.
     */
    @GetMapping("/welcome")
    public String welcome() {
        return "welcome";
    }
}

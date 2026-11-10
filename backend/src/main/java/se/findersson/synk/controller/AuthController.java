package se.findersson.synk.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.findersson.synk.security.AuthenticatedUser;

/**
 * Login and logout themselves are handled by Spring Security filters
 * (POST /api/auth/login and POST /api/auth/logout, see WebSecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record MeResponse(UUID id, String name) {}

    /** The logged-in user. Answers 401 when nobody is logged in. */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal final AuthenticatedUser user) {
        return new MeResponse(user.getId(), user.getUsername());
    }

}

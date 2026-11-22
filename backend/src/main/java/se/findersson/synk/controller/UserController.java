package se.findersson.synk.controller;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.findersson.synk.user.AppUserRepository;

/** The users, for the form fields that have one answer per person. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    public record UserResponse(UUID id, String name) {}

    private final AppUserRepository users;

    public UserController(final AppUserRepository users) {
        this.users = users;
    }

    /** Sorted by name, so the per-person fields always come in the same order. */
    @GetMapping
    public List<UserResponse> list() {
        return users.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getName()))
                .sorted(Comparator.comparing(UserResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

}

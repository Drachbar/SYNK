package se.findersson.synk.controller;

import java.time.Instant;

import jakarta.persistence.PersistenceException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import se.findersson.synk.form.FormDefinition;
import se.findersson.synk.form.FormService;
import se.findersson.synk.form.FormVersion;
import se.findersson.synk.security.AuthenticatedUser;

/** Versions of the weekly form. The API is described in docs/api.md. */
@RestController
@RequestMapping("/api/forms")
public class FormController {

    public record FormResponse(int version, FormDefinition definition, Instant createdAt) {

        static FormResponse of(final FormVersion form) {
            return new FormResponse(form.getVersion(), form.getDefinition(), form.getCreatedAt());
        }
    }

    public record ErrorResponse(String message) {}

    private final FormService service;

    public FormController(final FormService service) {
        this.service = service;
    }

    /** The version new weeks start with. */
    @GetMapping("/latest")
    public FormResponse latest() {
        return FormResponse.of(service.latest());
    }

    @GetMapping("/{version}")
    public ResponseEntity<FormResponse> get(@PathVariable final int version) {
        return ResponseEntity.of(service.find(version).map(FormResponse::of));
    }

    /** Saves a new version, or returns the latest one if nothing changed. */
    @PostMapping
    public FormResponse create(@RequestBody final FormDefinition definition,
            @AuthenticationPrincipal final AuthenticatedUser user) {
        return FormResponse.of(service.create(definition, user.getId()));
    }

    @ExceptionHandler(FormDefinition.InvalidFormException.class)
    ResponseEntity<ErrorResponse> invalid(final FormDefinition.InvalidFormException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    /** Someone else saved a version with the same number at the same time. */
    @ExceptionHandler({DataIntegrityViolationException.class, PersistenceException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    void conflict() {
    }

}

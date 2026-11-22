package se.findersson.synk.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import se.findersson.synk.entry.Entry;
import se.findersson.synk.entry.EntryRepository;
import se.findersson.synk.entry.EntryService;
import se.findersson.synk.security.AuthenticatedUser;

/** The shared weekly notes. The API is described in docs/api.md. */
@RestController
@RequestMapping("/api/entries")
public class EntryController {

    public record EntrySummary(int isoYear, int isoWeek, Instant updatedAt, String updatedBy) {}

    public record EntryResponse(int isoYear, int isoWeek, int schemaVersion, Map<String, String> content,
            int version, Instant updatedAt, String updatedBy) {

        static EntryResponse of(final Entry entry) {
            return new EntryResponse(entry.getIsoYear(), entry.getIsoWeek(), entry.getSchemaVersion(),
                    entry.getContent(), entry.getVersion(), entry.getUpdatedAt(), entry.getUpdatedBy().getName());
        }
    }

    /** {@code version} is the version the edit is based on, or null when there was no note yet. */
    public record SaveRequest(Integer version, int schemaVersion, Map<String, String> content) {}

    private final EntryService service;
    private final EntryRepository entries;

    public EntryController(final EntryService service, final EntryRepository entries) {
        this.service = service;
        this.entries = entries;
    }

    /** All weeks that have a note, newest first. */
    @GetMapping
    public List<EntrySummary> list() {
        return entries.findAllByOrderByIsoYearDescIsoWeekDesc().stream()
                .map(e -> new EntrySummary(e.getIsoYear(), e.getIsoWeek(), e.getUpdatedAt(), e.getUpdatedBy().getName()))
                .toList();
    }

    @GetMapping("/{isoYear}/{isoWeek}")
    public ResponseEntity<EntryResponse> get(@PathVariable final int isoYear, @PathVariable final int isoWeek) {
        return ResponseEntity.of(service.find(isoYear, isoWeek).map(EntryResponse::of));
    }

    @PutMapping("/{isoYear}/{isoWeek}")
    public EntryResponse save(@PathVariable final int isoYear, @PathVariable final int isoWeek,
            @RequestBody final SaveRequest request, @AuthenticationPrincipal final AuthenticatedUser user) {
        if (request.content() == null) {
            throw new BadRequestException();
        }
        return EntryResponse.of(service.save(isoYear, isoWeek, request.version(), request.schemaVersion(),
                request.content(), user.getId()));
    }

    private static class BadRequestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    @ExceptionHandler({EntryService.InvalidWeekException.class, BadRequestException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    void badRequest() {
    }

    /** Someone else saved first: by version check, by @Version, or by both creating the same week. */
    @ExceptionHandler({EntryService.ConflictException.class, OptimisticLockingFailureException.class,
            DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    void conflict() {
    }

}

package se.findersson.synk.entry;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.findersson.synk.form.FormDefinition;
import se.findersson.synk.form.FormService;
import se.findersson.synk.user.AppUserRepository;

@Service
public class EntryService {

    /** The note was saved by someone else after the caller loaded it. */
    public static class ConflictException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    /** The year/week pair is not a real ISO week. */
    public static class InvalidWeekException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    /** The form version does not exist, or the content has answers to fields that are not in it. */
    public static class InvalidContentException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private final EntryRepository entries;
    private final AppUserRepository users;
    private final FormService forms;

    public EntryService(final EntryRepository entries, final AppUserRepository users, final FormService forms) {
        this.entries = entries;
        this.users = users;
        this.forms = forms;
    }

    @Transactional(readOnly = true)
    public Optional<Entry> find(final int isoYear, final int isoWeek) {
        requireValidWeek(isoYear, isoWeek);
        return entries.findByIsoYearAndIsoWeek(isoYear, isoWeek);
    }

    /** The latest note before the given week, which need not be the week right before. */
    @Transactional(readOnly = true)
    public Optional<Entry> findPrevious(final int isoYear, final int isoWeek) {
        requireValidWeek(isoYear, isoWeek);
        return entries.findBefore(isoYear, isoWeek, Limit.of(1)).stream().findFirst();
    }

    /**
     * Creates or updates the week's note.
     *
     * @param expectedVersion the version the caller based its edit on, or {@code null} if it
     *                        saw no note for the week. Anything else than the current version
     *                        is a conflict.
     */
    @Transactional
    public Entry save(final int isoYear, final int isoWeek, final Integer expectedVersion,
            final int formVersion, final Map<String, Object> content, final UUID userId) {
        requireValidWeek(isoYear, isoWeek);
        final FormDefinition form = forms.find(formVersion)
                .orElseThrow(InvalidContentException::new).getDefinition();
        if (!content.keySet().stream().allMatch(form::hasField)) {
            throw new InvalidContentException();
        }
        final Optional<Entry> existing = entries.findByIsoYearAndIsoWeek(isoYear, isoWeek);
        final Integer currentVersion = existing.map(Entry::getVersion).orElse(null);
        if (!Objects.equals(currentVersion, expectedVersion)) {
            throw new ConflictException();
        }
        final Entry entry = existing.orElseGet(() -> new Entry(isoYear, isoWeek));
        entry.update(formVersion, content, users.findById(userId).orElseThrow());
        // Flush so that a concurrent save (version or unique constraint) fails here, inside the transaction.
        return entries.saveAndFlush(entry);
    }

    static void requireValidWeek(final int isoYear, final int isoWeek) {
        if (isoYear < 2000 || isoYear > 2999 || isoWeek < 1) {
            throw new InvalidWeekException();
        }
        // Dec 28 is always in the last ISO week of its week-based year.
        final long weeksInYear = LocalDate.of(isoYear, 12, 28).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        if (isoWeek > weeksInYear) {
            throw new InvalidWeekException();
        }
    }

}

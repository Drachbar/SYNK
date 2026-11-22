package se.findersson.synk.entry;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final EntryRepository entries;
    private final AppUserRepository users;

    public EntryService(final EntryRepository entries, final AppUserRepository users) {
        this.entries = entries;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Optional<Entry> find(final int isoYear, final int isoWeek) {
        requireValidWeek(isoYear, isoWeek);
        return entries.findByIsoYearAndIsoWeek(isoYear, isoWeek);
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
            final int schemaVersion, final Map<String, String> content, final UUID userId) {
        requireValidWeek(isoYear, isoWeek);
        final Optional<Entry> existing = entries.findByIsoYearAndIsoWeek(isoYear, isoWeek);
        final Integer currentVersion = existing.map(Entry::getVersion).orElse(null);
        if (!Objects.equals(currentVersion, expectedVersion)) {
            throw new ConflictException();
        }
        final Entry entry = existing.orElseGet(() -> new Entry(isoYear, isoWeek));
        entry.update(schemaVersion, content, users.findById(userId).orElseThrow());
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

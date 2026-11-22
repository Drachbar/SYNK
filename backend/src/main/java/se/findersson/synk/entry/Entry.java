package se.findersson.synk.entry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import se.findersson.synk.user.AppUser;

/**
 * The shared note for one ISO week. The answers live in {@code content}, keyed by the
 * field ids in frontend/src/lib/entry/fields.ts. See docs/databas.md.
 */
@Entity
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private int isoYear;

    private int isoWeek;

    private int schemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> content = new HashMap<>();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "updated_by_id")
    private AppUser updatedBy;

    /** Bumped on every save; a save based on an older version is rejected. */
    @Version
    private int version;

    @CreationTimestamp
    private Instant createdAt;

    /** Set in {@link #update}, so it only changes when someone actually saves. */
    private Instant updatedAt;

    protected Entry() {
    }

    public Entry(final int isoYear, final int isoWeek) {
        this.isoYear = isoYear;
        this.isoWeek = isoWeek;
    }

    public void update(final int schemaVersion, final Map<String, String> content, final AppUser by) {
        this.schemaVersion = schemaVersion;
        this.content = new HashMap<>(content);
        this.updatedBy = by;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public int getIsoYear() {
        return isoYear;
    }

    public int getIsoWeek() {
        return isoWeek;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public Map<String, String> getContent() {
        return content;
    }

    public AppUser getUpdatedBy() {
        return updatedBy;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}

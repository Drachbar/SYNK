package se.findersson.synk.form;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import se.findersson.synk.user.AppUser;

/** One version of the weekly form. Never changed once created; a change is a new version. */
@Entity
@Immutable
public class FormVersion {

    @Id
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    private FormDefinition definition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    @CreationTimestamp
    private Instant createdAt;

    protected FormVersion() {
    }

    public FormVersion(final int version, final FormDefinition definition, final AppUser createdBy) {
        this.version = version;
        this.definition = definition;
        this.createdBy = createdBy;
    }

    public int getVersion() {
        return version;
    }

    public FormDefinition getDefinition() {
        return definition;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}

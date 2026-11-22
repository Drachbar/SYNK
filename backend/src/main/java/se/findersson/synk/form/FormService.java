package se.findersson.synk.form;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.findersson.synk.user.AppUserRepository;

@Service
public class FormService {

    private final FormVersionRepository forms;
    private final AppUserRepository users;
    private final EntityManager em;

    public FormService(final FormVersionRepository forms, final AppUserRepository users, final EntityManager em) {
        this.forms = forms;
        this.users = users;
        this.em = em;
    }

    /** The version new weeks start with. The migrations always create at least one. */
    @Transactional(readOnly = true)
    public FormVersion latest() {
        return forms.findFirstByOrderByVersionDesc().orElseThrow();
    }

    @Transactional(readOnly = true)
    public Optional<FormVersion> find(final int version) {
        return forms.findById(version);
    }

    /**
     * Saves the definition as the next version. Returns the latest version unchanged if the
     * definition is identical to it, so saving without changes does not create versions.
     */
    @Transactional
    public FormVersion create(final FormDefinition definition, final UUID userId) {
        definition.validate();
        final FormVersion latest = latest();
        if (latest.getDefinition().equals(definition)) {
            return latest;
        }
        // persist, not save: save() would merge into an existing row with the same number.
        // Two saves at the same time get the same number, and the primary key rejects the second.
        final FormVersion created = new FormVersion(latest.getVersion() + 1, definition,
                users.findById(userId).orElseThrow());
        em.persist(created);
        em.flush();
        return created;
    }

}

package se.findersson.synk.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

/** Runs against the database from compose.yaml and removes what it created. */
@SpringBootTest
class AppUserTests {

    @Autowired
    private AppUserRepository users;

    @AfterEach
    void tearDown() {
        users.findByNameIgnoringCase("test-kalle").ifPresent(users::delete);
    }

    @Test
    void lookupIgnoresCase() {
        users.save(new AppUser("Test-Kalle", "x"));

        assertThat(users.findByNameIgnoringCase("test-kalle")).map(AppUser::getName).contains("Test-Kalle");
        assertThat(users.findByNameIgnoringCase("TEST-KALLE")).isPresent();
        assertThat(users.findByNameIgnoringCase("test-kall")).isEmpty();
    }

    @Test
    void namesDifferingOnlyInCaseAreNotAllowed() {
        users.save(new AppUser("test-kalle", "x"));

        assertThatThrownBy(() -> users.saveAndFlush(new AppUser("Test-Kalle", "y")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}

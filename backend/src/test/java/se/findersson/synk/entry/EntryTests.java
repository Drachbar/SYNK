package se.findersson.synk.entry;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import se.findersson.synk.security.AuthenticatedUser;
import se.findersson.synk.user.AppUser;
import se.findersson.synk.user.AppUserRepository;

/**
 * Runs against the database from compose.yaml, without a surrounding transaction so that
 * each request commits like in production. Uses years far ahead to stay clear of real notes,
 * and removes what it created afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EntryTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private EntryRepository entries;

    private AuthenticatedUser anna;
    private AuthenticatedUser bertil;

    @BeforeEach
    void setUp() {
        anna = new AuthenticatedUser(users.save(new AppUser("test-anna", "x")));
        bertil = new AuthenticatedUser(users.save(new AppUser("test-bertil", "x")));
    }

    @AfterEach
    void tearDown() {
        entries.deleteAll(entries.findAll().stream().filter(e -> e.getIsoYear() >= 2900).toList());
        users.deleteAllById(java.util.List.of(anna.getId(), bertil.getId()));
    }

    @Test
    void weekWithoutNoteIsNoContent() throws Exception {
        mvc.perform(get("/api/entries/2900/47").with(user(anna))).andExpect(status().isNoContent());
    }

    @Test
    void noteIsSharedBetweenUsers() throws Exception {
        mvc.perform(save(anna, 2900, 47, "null", "Lugn vecka"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.updatedBy").value("test-anna"));

        mvc.perform(get("/api/entries/2900/47").with(user(bertil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.summary").value("Lugn vecka"));

        mvc.perform(save(bertil, 2900, 47, "0", "Lugn vecka, men regnig"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.updatedBy").value("test-bertil"));

        mvc.perform(get("/api/entries").with(user(anna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.isoYear == 2900 && @.isoWeek == 47)].updatedBy").value("test-bertil"));
    }

    @Test
    void saveBasedOnOldVersionIsConflict() throws Exception {
        mvc.perform(save(anna, 2900, 47, "null", "Annas text")).andExpect(status().isOk());
        mvc.perform(save(anna, 2900, 47, "0", "Annas nya text")).andExpect(status().isOk());

        // Bertil opened the note before Anna's second save.
        mvc.perform(save(bertil, 2900, 47, "0", "Bertils text")).andExpect(status().isConflict());
        // Bertil opened the week before Anna created the note.
        mvc.perform(save(bertil, 2900, 47, "null", "Bertils text")).andExpect(status().isConflict());

        mvc.perform(get("/api/entries/2900/47").with(user(anna)))
                .andExpect(jsonPath("$.content.summary").value("Annas nya text"));
    }

    @Test
    void invalidWeekIsBadRequest() throws Exception {
        mvc.perform(get("/api/entries/2905/54").with(user(anna))).andExpect(status().isBadRequest());
        // 2904 has 52 ISO weeks, 2905 has 53.
        mvc.perform(save(anna, 2904, 53, "null", "x")).andExpect(status().isBadRequest());
        mvc.perform(save(anna, 2905, 53, "null", "x")).andExpect(status().isOk());
    }

    @Test
    void saveRequiresCsrfToken() throws Exception {
        mvc.perform(put("/api/entries/2900/47").with(user(anna))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":null,\"schemaVersion\":1,\"content\":{}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void weekValidation() {
        EntryService.requireValidWeek(2020, 53);
        assertThatThrownBy(() -> EntryService.requireValidWeek(2021, 53))
                .isInstanceOf(EntryService.InvalidWeekException.class);
        assertThatThrownBy(() -> EntryService.requireValidWeek(2026, 0))
                .isInstanceOf(EntryService.InvalidWeekException.class);
    }

    private static MockHttpServletRequestBuilder save(final AuthenticatedUser by, final int year, final int week,
            final String version, final String summary) {
        return put("/api/entries/" + year + "/" + week)
                .with(user(by)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + version + ",\"schemaVersion\":1,\"content\":{\"summary\":\"" + summary + "\"}}");
    }

}

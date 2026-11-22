package se.findersson.synk.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import se.findersson.synk.security.AuthenticatedUser;
import se.findersson.synk.user.AppUser;
import se.findersson.synk.user.AppUserRepository;

/** Runs against the database from compose.yaml and removes the versions it creates. */
@SpringBootTest
@AutoConfigureMockMvc
class FormTests {

    private static final String FORM = """
            {"sections": [{"title": "Test", "fields": [
              {"id": "plan", "type": "checklist", "label": "Plan"},
              {"id": "review", "type": "checklist", "label": "Förra veckan", "carryFrom": "plan"}
            ]}]}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private FormVersionRepository forms;

    private AuthenticatedUser anna;
    private final List<Integer> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        anna = new AuthenticatedUser(users.save(new AppUser("test-anna", "x")));
    }

    @AfterEach
    void tearDown() {
        forms.deleteAllById(created);
        users.deleteById(anna.getId());
    }

    @Test
    void seededVersionsExist() throws Exception {
        mvc.perform(get("/api/forms/1").with(user(anna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.definition.sections[0].fields[0].id").value("summary"));
        mvc.perform(get("/api/forms/2").with(user(anna)))
                .andExpect(jsonPath("$.definition.sections[0].fields[4].carryFrom").value("next_week"));
        mvc.perform(get("/api/forms/99999").with(user(anna))).andExpect(status().isNotFound());
    }

    @Test
    void savingCreatesTheNextVersionOnlyWhenChanged() throws Exception {
        final int before = latest();

        final int first = create(FORM);
        assertThat(first).isEqualTo(before + 1);
        assertThat(latest()).isEqualTo(first);

        // Same definition again: no new version.
        assertThat(create(FORM)).isEqualTo(first);
    }

    @Test
    void invalidDefinitionsAreRejected() throws Exception {
        mvc.perform(createRequest("{\"sections\": []}")).andExpect(status().isBadRequest());
        mvc.perform(createRequest("""
                {"sections": [{"title": "T", "fields": [
                  {"id": "a", "type": "text", "label": ""}, {"id": "a", "type": "text", "label": ""}]}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fält-id förekommer flera gånger: a"));
        mvc.perform(createRequest("""
                {"sections": [{"title": "T", "fields": [{"id": "a", "type": "radio", "label": ""}]}]}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(createRequest("""
                {"sections": [{"title": "T", "fields": [{"id": "a", "type": "checklist", "label": "", "carryFrom": "b"}]}]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creatingRequiresCsrfToken() throws Exception {
        mvc.perform(post("/api/forms").with(user(anna)).contentType(MediaType.APPLICATION_JSON).content(FORM))
                .andExpect(status().isForbidden());
    }

    private int latest() throws Exception {
        final String json = mvc.perform(get("/api/forms/latest").with(user(anna)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.version");
    }

    private int create(final String definition) throws Exception {
        final String json = mvc.perform(createRequest(definition))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        final int version = JsonPath.read(json, "$.version");
        created.add(version);
        return version;
    }

    private MockHttpServletRequestBuilder createRequest(final String definition) {
        return post("/api/forms").with(user(anna)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(definition);
    }

}

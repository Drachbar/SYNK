package se.findersson.synk.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import se.findersson.synk.config.WebSecurityConfig;
import se.findersson.synk.controller.AuthController;
import se.findersson.synk.controller.TestController;
import se.findersson.synk.user.AppUser;
import se.findersson.synk.user.AppUserRepository;

/** Runs against the web layer only, with the user table mocked, so no database is needed. */
@WebMvcTest(controllers = {AuthController.class, TestController.class})
@Import({WebSecurityConfig.class, AppUserDetailsService.class})
class AuthTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AppUserRepository users;

    @BeforeEach
    void setUp() {
        when(users.findByName("mattias"))
                .thenReturn(Optional.of(new AppUser("mattias", passwordEncoder.encode("hemligt"))));
    }

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/test")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void anyRequestHandsOutCsrfCookie() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(login("mattias", "hemligt")).andExpect(status().isForbidden());
    }

    @Test
    void loginWithWrongCsrfTokenIsForbidden() throws Exception {
        mvc.perform(login("mattias", "hemligt").cookie(csrfCookie()).header("X-XSRF-TOKEN", "fel"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithWrongPasswordOrUnknownUserIsUnauthorized() throws Exception {
        mvc.perform(withCsrf(login("mattias", "fel"))).andExpect(status().isUnauthorized());
        mvc.perform(withCsrf(login("okänd", "hemligt"))).andExpect(status().isUnauthorized());
    }

    @Test
    void loginGivesSessionThatWorksUntilLogout() throws Exception {
        final MockHttpSession session = (MockHttpSession) mvc.perform(withCsrf(login("mattias", "hemligt")))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession(false);
        assertThat(session).isNotNull();

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("mattias"));

        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(withCsrf(post("/api/auth/logout").session(session))).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void loginRouteIsNotSpringsGeneratedPage() throws Exception {
        final String body = mvc.perform(get("/login")).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Please sign in");
    }

    private static MockHttpServletRequestBuilder login(final String username, final String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", username)
                .param("password", password);
    }

    /** What the SPA does: read the XSRF-TOKEN cookie and send its value back in the X-XSRF-TOKEN header. */
    private MockHttpServletRequestBuilder withCsrf(final MockHttpServletRequestBuilder request) throws Exception {
        final Cookie cookie = csrfCookie();
        return request.cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue());
    }

    private Cookie csrfCookie() throws Exception {
        final Cookie cookie = mvc.perform(get("/api/auth/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return cookie;
    }

}

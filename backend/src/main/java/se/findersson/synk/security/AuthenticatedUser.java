package se.findersson.synk.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import se.findersson.synk.user.AppUser;

/**
 * The logged-in user, as stored in the session. Carries the id so that controllers
 * can look up the user's data without an extra query by name.
 *
 * <p>Instances are Java-serialized into the session table. Keep serialVersionUID when
 * making compatible changes, so sessions survive a deploy.
 */
public class AuthenticatedUser implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final UUID id;
    private final String name;
    private String passwordHash;

    public AuthenticatedUser(final AppUser user) {
        this.id = user.getId();
        this.name = user.getName();
        this.passwordHash = user.getPasswordHash();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    /** Called by Spring Security after login, so the hash is not kept in the session. */
    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

}

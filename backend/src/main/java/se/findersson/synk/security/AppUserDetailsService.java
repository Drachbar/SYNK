package se.findersson.synk.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import se.findersson.synk.user.AppUserRepository;

/** Looks up users in the app_user table when someone logs in. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(final AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) {
        return users.findByName(username)
                .map(AuthenticatedUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("No such user"));
    }

}

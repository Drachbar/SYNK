package se.findersson.synk.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    /**
     * Case-insensitive, so "Mattias" and "mattias" are the same user. Uses lower() to match
     * the unique index uq_app_user_name_lower.
     */
    @Query("select u from AppUser u where lower(u.name) = lower(:name)")
    Optional<AppUser> findByNameIgnoringCase(String name);

}

package se.findersson.synk.form;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FormVersionRepository extends JpaRepository<FormVersion, Integer> {

    Optional<FormVersion> findFirstByOrderByVersionDesc();

}

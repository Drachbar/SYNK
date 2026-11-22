package se.findersson.synk.entry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    Optional<Entry> findByIsoYearAndIsoWeek(int isoYear, int isoWeek);

    List<Entry> findAllByOrderByIsoYearDescIsoWeekDesc();

}

package se.findersson.synk.entry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    Optional<Entry> findByIsoYearAndIsoWeek(int isoYear, int isoWeek);

    List<Entry> findAllByOrderByIsoYearDescIsoWeekDesc();

    /** Notes before the given week, newest first. */
    @Query("""
            select e from Entry e
            where e.isoYear < :isoYear or (e.isoYear = :isoYear and e.isoWeek < :isoWeek)
            order by e.isoYear desc, e.isoWeek desc""")
    List<Entry> findBefore(int isoYear, int isoWeek, Limit limit);

}

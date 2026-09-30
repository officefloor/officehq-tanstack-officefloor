package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Note}. A Spring Data bean, injected into the OfficeFloor logic classes that
 * back the /api/notes routes.
 */
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * All notes, newest first. {@code at} is an ISO-8601 UTC string, so a descending sort on it is a
     * chronological newest-first sort. The UI scopes them to one target.
     */
    List<Note> findAllByOrderByAtDesc();
}

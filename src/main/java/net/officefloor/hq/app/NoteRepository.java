package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Note}. A Spring bean, injected into the OfficeFloor logic classes. */
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * One target's notes, newest first — the order the detail page shows. Ties on the timestamp fall
     * back to the id (also descending) so a stable "newest on top" order is always returned.
     */
    List<Note> findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(String targetType,
            Long targetId);
}

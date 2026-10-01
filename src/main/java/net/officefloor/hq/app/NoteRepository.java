package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Note}. Injected into the OfficeFloor REST logic classes. */
public interface NoteRepository extends JpaRepository<Note, Long> {

    /** The notes for one target, newest first, so a detail page shows its own notes on top. */
    List<Note> findByTargetTypeAndTargetIdOrderByAtDesc(String targetType, Long targetId);
}

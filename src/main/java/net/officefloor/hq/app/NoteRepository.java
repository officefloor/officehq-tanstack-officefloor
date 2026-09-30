package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Note}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface NoteRepository extends JpaRepository<Note, Long> {

    /** Every note on one target, newest first — the shape a target's notes render. */
    List<Note> findByTargetTypeAndTargetIdOrderByAtDescIdDesc(String targetType, Long targetId);
}

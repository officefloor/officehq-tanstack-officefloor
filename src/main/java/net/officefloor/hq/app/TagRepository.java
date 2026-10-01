package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Tag}. Injected into the OfficeFloor REST logic classes. */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /** Every tag, name order aside, oldest first so the add picker lists them in a stable order. */
    List<Tag> findAllByOrderByIdAsc();
}

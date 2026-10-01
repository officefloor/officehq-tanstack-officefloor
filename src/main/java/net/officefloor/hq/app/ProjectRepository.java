package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Project}. A Spring bean, injected into the OfficeFloor logic classes. */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** The projects of one client, in id order — the shape a client's detail page lists. */
    List<Project> findByClientIdOrderByIdAsc(Long clientId);

    /** How many projects one client owns — the figure the client detail badge surfaces. */
    long countByClientId(Long clientId);
}

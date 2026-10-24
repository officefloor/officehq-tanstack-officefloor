package net.officefloor.hq.app.crew;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    /** The crew's members, in the order they were added. */
    List<CrewMember> findByCrewIdOrderByIdAsc(Long crewId);

    boolean existsByCrewIdAndPersonId(Long crewId, Long personId);

    /** The person's memberships, in the order they were added. */
    List<CrewMember> findByPersonIdOrderByIdAsc(Long personId);
}

package net.officefloor.hq.app.pool;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoolMemberRepository extends JpaRepository<PoolMember, Long> {

    /** The pool's members, in the order they were added. */
    List<PoolMember> findByPoolIdOrderByIdAsc(Long poolId);

    boolean existsByPoolIdAndPersonId(Long poolId, Long personId);
}

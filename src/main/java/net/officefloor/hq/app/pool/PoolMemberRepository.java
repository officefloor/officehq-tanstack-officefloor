package net.officefloor.hq.app.pool;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoolMemberRepository extends JpaRepository<PoolMember, Long> {

    /** The pool's members, in the order they were added. */
    List<PoolMember> findByPoolIdOrderByIdAsc(Long poolId);

    boolean existsByPoolIdAndPersonId(Long poolId, Long personId);

    Optional<PoolMember> findByPoolIdAndPersonId(Long poolId, Long personId);

    /** The membership of the pool in another pool, if it is one. */
    Optional<PoolMember> findByMemberPoolId(Long memberPoolId);

    /** The person's memberships, in the order they were added. */
    List<PoolMember> findByPersonIdOrderByIdAsc(Long personId);
}

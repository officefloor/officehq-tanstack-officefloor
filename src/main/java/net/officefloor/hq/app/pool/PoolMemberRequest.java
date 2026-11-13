package net.officefloor.hq.app.pool;

import jakarta.validation.constraints.Size;

/** A member as the pool's row submits it: a person's id or another pool's id (one of them), and the share percent. */
public record PoolMemberRequest(Long personId, Long poolId, @Size(max = 30) String share) {
}

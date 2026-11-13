package net.officefloor.hq.app.pool;

import jakarta.validation.constraints.Size;

/** A member as the pool's row submits it: the person's id and the share percent. */
public record PoolMemberRequest(Long personId, @Size(max = 30) String share) {
}

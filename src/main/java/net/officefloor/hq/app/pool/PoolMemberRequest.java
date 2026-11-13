package net.officefloor.hq.app.pool;

import jakarta.validation.constraints.Size;

/**
 * A member as the pool's row submits it: a person's id or another pool's id (one of them), the share percent, and
 * the optional from-date and to-date, yyyy-mm-dd.
 */
public record PoolMemberRequest(Long personId, Long poolId, @Size(max = 30) String share, @Size(max = 30) String from,
        @Size(max = 30) String to) {
}

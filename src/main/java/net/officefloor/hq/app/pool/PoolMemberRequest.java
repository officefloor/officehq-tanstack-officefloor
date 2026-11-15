package net.officefloor.hq.app.pool;

/** Body of adding a member to a pool: the person and their share percent as typed. */
public record PoolMemberRequest(Long personId, String share) {
}

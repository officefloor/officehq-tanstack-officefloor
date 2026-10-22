package net.officefloor.hq.app.pool;

/** Body of a pool member add: who is adding it, the person and their share percent as typed. */
public record PoolMemberRequest(String user, Long personId, String share) {
}

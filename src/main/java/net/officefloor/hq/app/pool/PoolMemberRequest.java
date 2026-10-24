package net.officefloor.hq.app.pool;

/** The pool member entry as submitted: the person's id, or the member pool's id, and the share percent, as typed. */
public record PoolMemberRequest(Long personId, Long memberPoolId, String share) {
}

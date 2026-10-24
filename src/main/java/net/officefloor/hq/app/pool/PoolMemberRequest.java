package net.officefloor.hq.app.pool;

/** The pool member entry as submitted: the person's id and their share percent, as typed. */
public record PoolMemberRequest(Long personId, String share) {
}

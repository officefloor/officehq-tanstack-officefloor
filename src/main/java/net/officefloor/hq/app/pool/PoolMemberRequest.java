package net.officefloor.hq.app.pool;

/** The pool to add to, the id of the person joining it, their share percent as typed, and the signed-in user (`by`). */
public record PoolMemberRequest(String pool, Long person, String share, String by) {
}

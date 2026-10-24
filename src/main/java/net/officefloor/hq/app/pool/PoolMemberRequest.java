package net.officefloor.hq.app.pool;

/** The pool member entry as submitted: the person's id, or the member pool's id, the share percent, and the optional
 *  from- and to-dates, as typed. */
public record PoolMemberRequest(Long personId, Long memberPoolId, String share, String from, String to) {
}

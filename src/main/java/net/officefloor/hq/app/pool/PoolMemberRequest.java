package net.officefloor.hq.app.pool;

/** Body of a pool member add: who is adding it, the person or the pool (memberPoolId), their share percent as
 *  typed, and the optional from- and to-dates of the membership as typed. */
public record PoolMemberRequest(String user, Long personId, String share, Long memberPoolId, String from, String to) {
}

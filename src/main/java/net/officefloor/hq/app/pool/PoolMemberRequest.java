package net.officefloor.hq.app.pool;

/** Body of adding a member to a pool: the person, or else the pool ({@code memberPoolId}), their share
 *  percent as typed, and the optional dates (yyyy-mm-dd, blank: none) the membership runs from and to. */
public record PoolMemberRequest(Long personId, Long memberPoolId, String share, String from, String to) {
}

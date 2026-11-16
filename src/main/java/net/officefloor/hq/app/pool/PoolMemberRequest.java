package net.officefloor.hq.app.pool;

/** Body of adding a member to a pool: the pool's name, the person or the member pool's name (memberPool), their share
 *  percent (as typed), the membership's from-date and to-date (yyyy-MM-dd, blank for none), and who adds it. */
public record PoolMemberRequest(String pool, Long personId, String memberPool, String share, String from, String to,
        String by) {
}

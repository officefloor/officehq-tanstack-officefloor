package net.officefloor.hq.app.pool;

/** Body of a member added to a pool: the pool's name, the person (or memberPool, another pool's name),
 *  the share percent, who adds it and the optional from- and to-dates (yyyy-mm-dd). */
public record PoolMemberRequest(String pool, Long personId, String share, String user, String memberPool,
        String from, String to) {
}

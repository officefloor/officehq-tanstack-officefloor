package net.officefloor.hq.app.pool;

/** A pool member as the screen sends it: the pool's name, the person's id or a member pool's name, a share percent,
 * and optional from- and to-dates. */
public record PoolMemberRequest(String pool, String personId, String memberPool, String share, String from,
        String to) {
}

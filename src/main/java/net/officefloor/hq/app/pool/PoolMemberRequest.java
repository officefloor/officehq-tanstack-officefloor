package net.officefloor.hq.app.pool;

/** A pool member as the screen sends it: the pool's name, the person's id or a member pool's name, and a share percent. */
public record PoolMemberRequest(String pool, String personId, String memberPool, String share) {
}

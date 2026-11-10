package net.officefloor.hq.app.pool;

/** A pool member as the screen sends it: the pool's name, the person's id and a share percent. */
public record PoolMemberRequest(String pool, String personId, String share) {
}

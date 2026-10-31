package net.officefloor.hq.app.pool;

/** A pool's member as the screen sees it: the person, their share (two decimals) and whether retired. */
public record PoolMemberResponse(long personId, String name, String share, boolean retired) {
}

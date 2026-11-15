package net.officefloor.hq.app.pool;

/** A person in a pool, with their share percent (two decimals) and whether they are retired. */
public record PoolMember(long personId, String name, String share, boolean retired) {
}

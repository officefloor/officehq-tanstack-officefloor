package net.officefloor.hq.app.pool;

/** A person in a pool with their share percent (two decimals); retired when the person has a retirement date set. */
public record PoolMember(long person, String name, String share, boolean retired) {
}

package net.officefloor.hq.app.pool;

/** A member of a pool: the person (by id, and their name now), their share percent (two decimals) and
 *  whether they are retired (a retirement date set, whatever the date). */
public record PoolMember(long personId, String name, String share, boolean retired) {
}

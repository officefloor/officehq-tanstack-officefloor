package net.officefloor.hq.app.pool;

/**
 * Body of POST /api/sheets/{sheet}/pool-members: the pool, the member (a person's id, or another pool's name), their
 * share percent, and the optional from-date and to-date of the membership.
 */
public record PoolMemberRequest(String pool, Long person, String memberPool, String share, String from, String to) {
}

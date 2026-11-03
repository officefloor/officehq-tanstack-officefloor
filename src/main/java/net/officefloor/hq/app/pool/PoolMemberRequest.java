package net.officefloor.hq.app.pool;

public record PoolMemberRequest(String pool, Long personId, String share) {
}

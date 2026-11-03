package net.officefloor.hq.app.pool;

public record PoolMemberRequest(String pool, Long personId, String memberPool, String share, String from, String to) {
}

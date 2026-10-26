package net.officefloor.hq.app.pool;

/** A pool's name as typed, and the signed-in user (`by`) adding it. */
public record PoolRequest(String name, String by) {
}

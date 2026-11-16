package net.officefloor.hq.app.pool;

/** Body of adding a pool: its name, and who adds it. */
public record PoolRequest(String name, String by) {
}

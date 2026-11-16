package net.officefloor.hq.app.pool;

/** A pool to add once validated: its trimmed name, and who adds it. */
public record PoolValues(String name, String by) {
}

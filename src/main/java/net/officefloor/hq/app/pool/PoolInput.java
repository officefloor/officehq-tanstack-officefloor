package net.officefloor.hq.app.pool;

/** A pool to add, validated and trimmed, and the user adding it. */
public record PoolInput(String name, String user) {
}

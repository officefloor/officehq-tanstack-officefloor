package net.officefloor.hq.app.pool;

/** A pool request, validated: its name, trimmed and not blank. */
public record PoolInput(String name) {
}

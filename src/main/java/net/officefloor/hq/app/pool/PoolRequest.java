package net.officefloor.hq.app.pool;

/** Body of POST /api/sheets/{sheet}/pools: the name of the pool to add. */
public record PoolRequest(String name) {
}

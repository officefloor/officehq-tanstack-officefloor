package net.officefloor.hq.app.pool;

/** Body of POST /api/sheets/{sheetId}/pools: the pool's name and the user who adds it. */
public record PoolRequest(String name, String user) {
}

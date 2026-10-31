package net.officefloor.hq.app.pool;

/** Body of a pool added to the sheet: its name and who adds it. */
public record PoolRequest(String name, String user) {
}

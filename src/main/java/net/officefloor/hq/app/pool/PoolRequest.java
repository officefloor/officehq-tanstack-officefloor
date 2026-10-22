package net.officefloor.hq.app.pool;

/** Body of a pool add: who is adding it and the name as typed. */
public record PoolRequest(String user, String name) {
}

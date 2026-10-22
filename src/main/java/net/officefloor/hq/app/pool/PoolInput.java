package net.officefloor.hq.app.pool;

/** A validated pool add: who is adding it and the name, trimmed. */
public record PoolInput(String user, String name) {
}

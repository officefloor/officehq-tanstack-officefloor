package net.officefloor.hq.app.pool;

/** A pool name that passed validation: trimmed, not blank and not yet a pool on the sheet. */
public record ValidPool(String name, String user) {
}

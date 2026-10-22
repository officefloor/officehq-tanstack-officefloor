package net.officefloor.hq.app.person;

/** A validated transfer: the signed-in user, and the id of the sheet transferred to (trimmed). */
public record TransferInput(String user, String toSheet) {
}

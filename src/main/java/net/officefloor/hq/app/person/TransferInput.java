package net.officefloor.hq.app.person;

/** A validated transfer request: the id of the sheet to transfer the person to, trimmed. */
public record TransferInput(String toSheet) {
}

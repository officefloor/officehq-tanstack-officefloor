package net.officefloor.hq.app.person;

/** A validated transfer: the id of the sheet the person goes to, trimmed. */
public record TransferInput(String to) {
}

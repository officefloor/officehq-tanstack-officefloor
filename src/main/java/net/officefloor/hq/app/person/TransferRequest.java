package net.officefloor.hq.app.person;

/** The body of a transfer: the id of the sheet the person goes to. */
public record TransferRequest(String toSheetId) {
}

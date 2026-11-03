package net.officefloor.hq.app.person;

/** The transfer form: the id of the sheet to transfer this person to. */
public record TransferRequest(String toSheetId) {
}

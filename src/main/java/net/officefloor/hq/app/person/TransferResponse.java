package net.officefloor.hq.app.person;

/** The answer to a transfer: the sheet the person went to and their id there. */
public record TransferResponse(String to, long id) {
}

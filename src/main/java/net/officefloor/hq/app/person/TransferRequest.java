package net.officefloor.hq.app.person;

/** Body of a transfer: the id of the sheet the person is transferred to. */
public record TransferRequest(String to) {
}

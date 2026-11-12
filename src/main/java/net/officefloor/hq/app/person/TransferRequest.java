package net.officefloor.hq.app.person;

/** Body of a transfer: the id of the sheet the person goes to, and who transfers them. */
public record TransferRequest(String to, String user) {
}

package net.officefloor.hq.app.person;

/** The id of the sheet to transfer the person to, and the signed-in user (`by`) doing it. */
public record TransferRequest(String to, String by) {
}

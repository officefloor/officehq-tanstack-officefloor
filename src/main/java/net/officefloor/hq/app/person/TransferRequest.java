package net.officefloor.hq.app.person;

/** Body of transferring a person to another sheet: the id of the sheet they go to, and who transfers them. */
public record TransferRequest(String toSheet, String by) {
}

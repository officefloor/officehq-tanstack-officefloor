package net.officefloor.hq.app.person;

/** Body of transferring a person to another sheet: that sheet's id, and who makes the change. */
public record TransferRequest(String to, String user) {
}

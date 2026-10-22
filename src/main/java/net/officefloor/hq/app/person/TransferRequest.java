package net.officefloor.hq.app.person;

/** Body of transferring a person to another sheet: who is doing it, and the id of the other sheet. */
public record TransferRequest(String user, String toSheet) {
}

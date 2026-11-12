package net.officefloor.hq.app.claim;

/** Body of a claim status change, as sent: the status to move to and who makes the change. */
public record StatusRequest(String status, String user) {
}

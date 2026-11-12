package net.officefloor.hq.app.claim;

/** Body of a claim status change, as sent: the status to move to and who makes the change; for a
 *  steward's review of a PENDING claim, {@code review} true (status SETTLED to approve, OPEN to reject)
 *  and the reason given for a rejection; for a settle, the date (yyyy-MM-dd) it is made as of, if any. */
public record StatusRequest(String status, String user, Boolean review, String reason, String on) {
}

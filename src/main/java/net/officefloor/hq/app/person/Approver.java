package net.officefloor.hq.app.person;

/** That {@code user} is the approver of the person {@code personId}: they may approve or reject the person's PENDING
 *  claims as a steward may. */
public record Approver(long personId, String user) {
}

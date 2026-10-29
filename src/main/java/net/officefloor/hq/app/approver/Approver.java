package net.officefloor.hq.app.approver;

/** A person's approver: the user name who may approve or reject the person's PENDING claims as a steward may. */
public record Approver(long personId, String user) {
}

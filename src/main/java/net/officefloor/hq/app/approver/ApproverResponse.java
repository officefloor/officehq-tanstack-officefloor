package net.officefloor.hq.app.approver;

/** A person's approver: the user name who may approve or reject their PENDING claims. */
public record ApproverResponse(long personId, String approver) {
}

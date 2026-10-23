package net.officefloor.hq.app.approver;

/** Body of setting a person's approver: who is doing it, and the user name of the approver (blank clears it). */
public record ApproverRequest(String user, String approver) {
}

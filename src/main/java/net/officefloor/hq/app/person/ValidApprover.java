package net.officefloor.hq.app.person;

/** An approver that passed validation: the user name ("" to clear it) and who sets it. */
public record ValidApprover(String approver, String user) {
}

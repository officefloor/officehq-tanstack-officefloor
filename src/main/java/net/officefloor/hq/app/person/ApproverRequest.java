package net.officefloor.hq.app.person;

/** Body of setting a person's approver, as typed: the approver's user name (blank to clear it) and who sets it. */
public record ApproverRequest(String approver, String user) {
}

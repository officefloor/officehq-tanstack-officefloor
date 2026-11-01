package net.officefloor.hq.app.person;

/** Body of setting a person's approver, as typed: the approver's user name (blank: none) and who makes the change. */
public record ApproverRequest(String approver, String user) {
}

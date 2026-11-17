package net.officefloor.hq.app.person;

/** Body of setting a person's approver: the user to make approver, and who sets it. */
public record ApproverRequest(String user, String by) {
}

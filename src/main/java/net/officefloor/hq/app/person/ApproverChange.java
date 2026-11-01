package net.officefloor.hq.app.person;

/** A validated approver: the user name, trimmed (null: none), and who makes the change. */
public record ApproverChange(String approver, String user) {
}

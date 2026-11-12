package net.officefloor.hq.app.person;

/** A validated approver: the approver's user name trimmed (empty to clear it) and who sets it. */
public record ApproverInput(String approver, String user) {
}

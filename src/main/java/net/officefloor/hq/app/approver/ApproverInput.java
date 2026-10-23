package net.officefloor.hq.app.approver;

/** A validated approver: the signed-in user and the trimmed user name of the approver (blank for none). */
public record ApproverInput(String user, String approver) {
}

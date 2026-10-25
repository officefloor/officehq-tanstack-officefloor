package net.officefloor.hq.app.person;

/** A validated approver request: the trimmed user name (null: no approver). */
public record ApproverInput(String user) {
}

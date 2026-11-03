package net.officefloor.hq.app.person;

/** A validated approver form: a user name without spaces, or empty for none. */
public record ApproverInput(String user) {
}

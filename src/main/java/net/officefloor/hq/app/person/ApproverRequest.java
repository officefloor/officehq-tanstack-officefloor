package net.officefloor.hq.app.person;

/** The approver form: the user name to approve the person's pending claims (blank: none). */
public record ApproverRequest(String user) {
}

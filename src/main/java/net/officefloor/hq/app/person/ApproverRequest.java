package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people/{person}/approver: the approver's user name (blank clears it). */
public record ApproverRequest(String user) {
}

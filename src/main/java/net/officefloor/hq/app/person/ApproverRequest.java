package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/approver: the approver's user name and who sets it. */
public record ApproverRequest(String approver, String user) {
}

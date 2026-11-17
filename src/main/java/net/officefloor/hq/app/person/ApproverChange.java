package net.officefloor.hq.app.person;

/** An approver change once validated: the user made approver, and who sets it. */
public record ApproverChange(String user, String by) {
}

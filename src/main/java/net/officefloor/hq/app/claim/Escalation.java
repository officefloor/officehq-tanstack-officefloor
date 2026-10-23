package net.officefloor.hq.app.claim;

/** Whether a reminder escalated its claim (the first reminder at least the sheet's escalation gap after the one
 *  before it). */
public record Escalation(boolean escalated) {
}

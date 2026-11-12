package net.officefloor.hq.app.role;

/** Body of POST /api/sheets/{sheetId}/delegations: the user, the from- and to-date (yyyy-mm-dd; a blank
 *  from-date is the sheet's today) and the steward who delegates. */
public record DelegationRequest(String user, String from, String to, String by) {
}

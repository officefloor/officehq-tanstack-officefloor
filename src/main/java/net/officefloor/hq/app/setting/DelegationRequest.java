package net.officefloor.hq.app.setting;

/** Body of POST /api/sheets/{sheetId}/delegations: the user given the steward role, its from-date and to-date
 *  (blank: the sheet's today), and the steward who delegates. */
public record DelegationRequest(String name, String from, String to, String user) {
}

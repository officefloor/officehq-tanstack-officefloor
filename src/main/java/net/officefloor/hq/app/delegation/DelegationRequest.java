package net.officefloor.hq.app.delegation;

/** Body of POST /api/sheets/{sheet}/delegations: the user, and the from-date and to-date (yyyy-mm-dd). */
public record DelegationRequest(String user, String from, String to) {
}

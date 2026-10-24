package net.officefloor.hq.app.role;

/** The delegate form as submitted: the user name and the from- and to-dates (YYYY-MM-DD; blank: the sheet's today). */
public record DelegationRequest(String user, String from, String to) {
}

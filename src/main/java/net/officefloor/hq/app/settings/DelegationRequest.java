package net.officefloor.hq.app.settings;

/** The delegate form: the user, the from-date (blank: the sheet's today) and the to-date. */
public record DelegationRequest(String user, String from, String to) {
}

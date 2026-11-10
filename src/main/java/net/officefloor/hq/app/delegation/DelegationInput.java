package net.officefloor.hq.app.delegation;

/** A delegation request, trimmed: the dates as typed (blank from: the sheet's today). */
public record DelegationInput(String user, String from, String to) {
}

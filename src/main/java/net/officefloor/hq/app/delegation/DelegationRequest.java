package net.officefloor.hq.app.delegation;

/** Body of a delegation: who is making it (a steward), the user it is for, and its from- and to-dates. */
public record DelegationRequest(String user, String name, String from, String to) {
}

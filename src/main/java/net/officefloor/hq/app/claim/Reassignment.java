package net.officefloor.hq.app.claim;

/** The names of the person a claim was moved from and the person it was moved to, for the audit line. */
public record Reassignment(String from, String to) {
}

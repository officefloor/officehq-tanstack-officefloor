package net.officefloor.hq.app.person;

/**
 * A person's own terms (days; null: none), in force from the from-date (yyyy-mm-dd; null when they apply from the
 * beginning of time).
 */
public record TermsFrom(Integer terms, String from) {
}

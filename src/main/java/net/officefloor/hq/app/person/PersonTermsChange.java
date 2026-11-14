package net.officefloor.hq.app.person;

/**
 * Whether an edit recorded terms of the person's own (terms other than those the row showed, or a from-date
 * given), and whether it changed their contact.
 */
public record PersonTermsChange(boolean changed, boolean contactChanged) {
}

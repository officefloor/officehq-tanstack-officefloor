package net.officefloor.hq.app.person;

/**
 * Whether an edit changed the person's own terms from those they had before the save, and whether it changed their
 * contact.
 */
public record PersonTermsChange(boolean changed, boolean contactChanged) {
}

package net.officefloor.hq.app.person;

/** Whether an edit changed the person's own terms from what they were before the save. */
public record TermsEdit(boolean changed) {
}

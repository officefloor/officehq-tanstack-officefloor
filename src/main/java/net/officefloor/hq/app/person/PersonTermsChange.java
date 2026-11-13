package net.officefloor.hq.app.person;

/** Whether an edit changed the person's own terms from those they had before the save. */
public record PersonTermsChange(boolean changed) {
}

package net.officefloor.hq.app.person;

/** Whether an edit changed the person's own terms from those the row showed before the save. */
public record TermsEdit(boolean changed) {}

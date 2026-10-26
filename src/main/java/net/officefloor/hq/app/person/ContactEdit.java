package net.officefloor.hq.app.person;

/** Whether an edit changed the person's contact from what it was before the save. */
public record ContactEdit(boolean changed) {
}

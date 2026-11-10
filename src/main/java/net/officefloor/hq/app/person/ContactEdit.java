package net.officefloor.hq.app.person;

/** Whether an edit changed the person's contact from the one stored before the save. */
public record ContactEdit(boolean changed) {}

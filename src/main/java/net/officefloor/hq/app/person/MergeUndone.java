package net.officefloor.hq.app.person;

/** The person a merge undo restored (wrapped, so it is told apart from the person merged into). */
public record MergeUndone(Person person) {
}

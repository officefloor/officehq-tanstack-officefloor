package net.officefloor.hq.app.person;

/** The person another was merged into (wrapped, so it is told apart from the person merged). */
public record MergedInto(Person person) {
}

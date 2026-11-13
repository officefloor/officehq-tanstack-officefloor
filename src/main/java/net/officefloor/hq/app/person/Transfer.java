package net.officefloor.hq.app.person;

/**
 * A person's transfer: the person as they stand on their sheet, the id of the sheet they go to, and the person there
 * they are merged into on arrival (one with their name or one of their aliases), or null when they arrive as a new
 * person.
 */
public record Transfer(Person person, String to, Person into) {
}

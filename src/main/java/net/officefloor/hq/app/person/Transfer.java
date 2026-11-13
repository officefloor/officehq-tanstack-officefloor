package net.officefloor.hq.app.person;

/** A person's transfer: the person as they stand on their sheet, and the id of the sheet they go to. */
public record Transfer(Person person, String to) {
}

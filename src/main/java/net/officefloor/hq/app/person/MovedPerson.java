package net.officefloor.hq.app.person;

/** The person moved, with their new position in the list (counting from 1). */
public record MovedPerson(Person person, int position) {
}

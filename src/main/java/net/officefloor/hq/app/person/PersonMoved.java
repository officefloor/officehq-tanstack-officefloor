package net.officefloor.hq.app.person;

/** A person just moved in the list and their new position there, counting from 1. */
public record PersonMoved(Person person, int position) {
}

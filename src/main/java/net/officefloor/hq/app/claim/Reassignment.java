package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/** A claim's move from the person it was with to the person it goes to. */
public record Reassignment(Person from, Person to) {
}

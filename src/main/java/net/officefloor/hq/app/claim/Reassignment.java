package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/** A claim's move: the person it is moved from and the person it is moved to. */
public record Reassignment(Person from, Person to) {
}

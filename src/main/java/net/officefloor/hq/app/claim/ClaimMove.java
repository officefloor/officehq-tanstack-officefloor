package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/** A claim's move: the person it leaves and the person it moves to. */
public record ClaimMove(Person from, Person to) {
}

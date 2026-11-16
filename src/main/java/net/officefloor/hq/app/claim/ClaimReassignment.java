package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/** A move of a claim once checked: the person it moves from, the person it moves to, and who moves it. */
public record ClaimReassignment(Person from, Person to, String by) {
}

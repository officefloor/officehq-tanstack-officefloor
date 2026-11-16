package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.person.Person;

/** The person just named as a patron (its own type, so it does not clash with the person being added to). */
public record PatronOf(Person patron) {
}

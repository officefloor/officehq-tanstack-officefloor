package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.person.Person;

/** The person a quick-add lands on, and whether the quick-add created them. */
public record QuickAddPerson(Person person, boolean added) {
}

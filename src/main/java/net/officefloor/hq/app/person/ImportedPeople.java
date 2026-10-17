package net.officefloor.hq.app.person;

import java.util.List;

/** The people of an import that passed its checks, in the order they were pasted. */
public record ImportedPeople(List<Person> people) {
}

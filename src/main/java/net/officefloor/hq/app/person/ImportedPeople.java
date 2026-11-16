package net.officefloor.hq.app.person;

import java.util.List;

/** The imported people's values in the order pasted, and who imports them. */
public record ImportedPeople(List<PersonValues> people, String by) {
}

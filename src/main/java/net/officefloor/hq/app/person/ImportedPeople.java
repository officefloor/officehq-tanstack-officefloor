package net.officefloor.hq.app.person;

import java.util.List;

/** The imported lines, in order, each checked under the person form's rules. */
public record ImportedPeople(List<PersonRequest> people) {
}

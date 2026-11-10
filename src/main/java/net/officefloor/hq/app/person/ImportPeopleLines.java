package net.officefloor.hq.app.person;

import java.util.List;

/** The imported lines, each checked by the person form's rules, in the order they were pasted. */
public record ImportPeopleLines(List<PersonRequest> lines) {
}

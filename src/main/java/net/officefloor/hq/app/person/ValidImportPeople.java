package net.officefloor.hq.app.person;

import java.util.List;

/** The import's lines, each checked by the person form's rules, and the user adding them. */
public record ValidImportPeople(List<PersonRequest> people, String user) {
}

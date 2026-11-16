package net.officefloor.hq.app.person;

import java.util.List;

/** For each imported line, in the order pasted, the person on the sheet it updates (matched by name or alias); null
 *  for a line that adds a person. */
public record ImportMatches(List<Person> people) {
}

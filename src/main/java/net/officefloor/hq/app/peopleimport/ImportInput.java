package net.officefloor.hq.app.peopleimport;

import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;

/** A validated import: each line as the person form would take it, in the order pasted, with the person on the
 *  sheet it updates (null for a line that adds). */
public record ImportInput(List<Line> lines) {
    public record Line(PersonRequest request, Person existing) {
    }
}

package net.officefloor.hq.app.person;

import java.util.List;

/** Every imported line, checked by the person form's rules, in the order pasted. */
public record ImportedPeople(List<Line> lines) {

    /** A checked line, with the person on the sheet its name matches (by name or alias), or null when it adds one. */
    public record Line(PersonRequest request, Person existing) {
    }
}

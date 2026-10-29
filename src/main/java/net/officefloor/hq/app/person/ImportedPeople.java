package net.officefloor.hq.app.person;

import java.util.List;

/**
 * Every line of an import of people, checked by the person form's rules, in the order typed. A line whose name
 * matches a person on the sheet (by name or alias) carries that person, to be updated; otherwise it is null, to add.
 */
public record ImportedPeople(List<Line> people) {

    public record Line(PersonRequest request, Person existing) {
    }
}

package net.officefloor.hq.app.person;

import java.util.List;

/** A person as shown; retiredFrom is yyyy-mm-dd, or null while not retired; held while on hold; patronIds the people named as their patrons. */
public record PersonResponse(long id, String name, String grade, long version, String retiredFrom,
        boolean held, List<Long> patronIds) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion(),
                person.getRetiredFrom() == null ? null : person.getRetiredFrom().toString(), person.isHeld(),
                person.getPatronIds().stream().sorted().toList());
    }
}

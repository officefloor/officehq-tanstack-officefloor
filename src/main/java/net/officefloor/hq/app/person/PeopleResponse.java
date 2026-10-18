package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A sheet's people, in the order they were added, each with the grade in force on the sheet's today. */
public record PeopleResponse(List<PersonResponse> people) {

    public static PeopleResponse from(List<Person> people, LocalDate today) {
        return new PeopleResponse(people.stream().map((p) -> PersonResponse.from(p, today)).toList());
    }
}

package net.officefloor.hq.app.person;

import java.util.List;

/** A sheet's people, in the order they were added. */
public record PeopleResponse(List<PersonResponse> people) {

    public static PeopleResponse from(List<Person> people) {
        return new PeopleResponse(people.stream().map(PersonResponse::from).toList());
    }
}

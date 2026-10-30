package net.officefloor.hq.app.person;

import java.util.List;

public record PersonResponse(String id, String name, String grade) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.getGrade());
    }

    public static List<PersonResponse> from(List<Person> people) {
        return people.stream().map(PersonResponse::from).toList();
    }
}

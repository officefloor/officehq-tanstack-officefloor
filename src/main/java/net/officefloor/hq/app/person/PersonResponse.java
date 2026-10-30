package net.officefloor.hq.app.person;

import java.util.List;

public record PersonResponse(String id, String name, String grade, int version) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.getGrade(), person.getVersion());
    }

    public static List<PersonResponse> from(List<Person> people) {
        return people.stream().map(PersonResponse::from).toList();
    }
}

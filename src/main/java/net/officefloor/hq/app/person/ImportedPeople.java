package net.officefloor.hq.app.person;

import java.util.List;

/** Every line of an import of people, checked by the person form's rules, in the order typed. */
public record ImportedPeople(List<PersonRequest> people) {
}

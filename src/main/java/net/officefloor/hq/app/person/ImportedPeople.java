package net.officefloor.hq.app.person;

import java.util.List;

/** Every imported line, checked by the person form's rules, in the order pasted. */
public record ImportedPeople(List<PersonRequest> lines) {
}

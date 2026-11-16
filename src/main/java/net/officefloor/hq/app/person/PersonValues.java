package net.officefloor.hq.app.person;

import java.util.Set;

/** A person's values once validated, and who gives them; for an edit, the version it was made from (null for an add). */
public record PersonValues(String name, String grade, String by, Long version) {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** The request's values by the person form's rules (see ValidatePerson). */
    public static PersonValues from(PersonRequest request) throws InvalidPersonException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("Grade must be A, B or C");
        }
        return new PersonValues(name, grade, request.by() == null ? "" : request.by().trim(), request.version());
    }
}

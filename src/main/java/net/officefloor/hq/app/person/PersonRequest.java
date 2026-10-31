package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;

/** Body of a person add or edit: the name, the grade (A, B or C), who makes the change and, for an
 *  edit, the version of the person the editor loaded (null: no check). */
public record PersonRequest(String name, String grade, String user, Integer version) {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** The request as the person rules accept it, or refused. */
    public static PersonRequest checked(PersonRequest request) throws InvalidSheetRequestException {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new InvalidSheetRequestException("A name is required");
        }
        if (request.grade() == null || !GRADES.contains(request.grade())) {
            throw new InvalidSheetRequestException("The grade must be A, B or C");
        }
        // Names are kept without the spaces around them.
        return new PersonRequest(request.name().trim(), request.grade(), request.user(), request.version());
    }
}

package net.officefloor.hq.app.person;

import jakarta.validation.constraints.Size;

/**
 * A person's fields as the form submits them: a name (required) and a grade (A, B or C). An edit
 * also sends the version of the person its editor loaded; an add sends none.
 */
public record PersonRequest(@Size(max = 200) String name, @Size(max = 10) String grade, Integer version) {
}

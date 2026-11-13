package net.officefloor.hq.app.person;

import jakarta.validation.constraints.Size;

/** A person's fields as the form submits them: a name (required) and a grade (A, B or C). */
public record PersonRequest(@Size(max = 200) String name, @Size(max = 10) String grade) {
}

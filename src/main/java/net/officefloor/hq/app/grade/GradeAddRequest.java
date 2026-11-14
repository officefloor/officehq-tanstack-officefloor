package net.officefloor.hq.app.grade;

import jakarta.validation.constraints.Size;

/** The code the add-grade entry submits. */
public record GradeAddRequest(@Size(max = 30) String grade) {
}

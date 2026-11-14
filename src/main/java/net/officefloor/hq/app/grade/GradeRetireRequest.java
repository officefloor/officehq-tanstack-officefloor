package net.officefloor.hq.app.grade;

import jakarta.validation.constraints.Size;

/** The grade the retire select submits. */
public record GradeRetireRequest(@Size(max = 10) String grade) {
}

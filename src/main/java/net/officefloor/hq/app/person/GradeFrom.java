package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A grade a person was given, in force from its from-date (null: from the beginning of time). */
public record GradeFrom(String grade, LocalDate from) {
}

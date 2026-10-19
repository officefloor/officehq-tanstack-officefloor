package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** One of a person's grades, in force from its from-date (null: from the beginning of time). */
public record DatedGrade(String grade, LocalDate from) {
}

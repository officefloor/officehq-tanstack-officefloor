package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A grade a person was given from a date on. */
public record DatedGrade(LocalDate from, String grade) {
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A grade a person has from a date until their next one's; a null {@code from} applies from the beginning of time. */
public record PersonGrade(String grade, LocalDate from) {
}

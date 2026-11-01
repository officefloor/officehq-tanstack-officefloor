package net.officefloor.hq.app.person;

/** One of a person's grades, in force from its from-date (yyyy-mm-dd; null when it applies from the beginning of time). */
public record GradeFrom(String grade, String from) {
}

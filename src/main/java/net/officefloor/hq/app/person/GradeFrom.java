package net.officefloor.hq.app.person;

/** A grade a person was given, in force from its from-date (yyyy-MM-dd; null: from the beginning of time). */
public record GradeFrom(String grade, String from) {
}

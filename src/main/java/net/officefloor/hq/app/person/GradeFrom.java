package net.officefloor.hq.app.person;

/** One of a person's grades: in force from its from-date (yyyy-mm-dd; null for the first grade they
 *  were given, which applies from the beginning of time) until the next one's. */
public record GradeFrom(String grade, String from) {
}

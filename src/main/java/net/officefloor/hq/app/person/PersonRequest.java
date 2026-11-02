package net.officefloor.hq.app.person;

/** The person form; {@code gradeFrom} is the date the grade is in force from (yyyy-mm-dd; blank for the
 *  sheet's today; an edit only), {@code version} the version the editor loaded (an edit only). */
public record PersonRequest(String name, String grade, Long version, String gradeFrom) {
}

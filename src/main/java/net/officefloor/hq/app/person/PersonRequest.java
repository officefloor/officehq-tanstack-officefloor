package net.officefloor.hq.app.person;

/** The person form: the request body to add or edit a person. An edit carries the {@code version}
 *  of the person its form loaded (null when adding). {@code gradeFrom} (yyyy-MM-dd; blank: the sheet's
 *  today) is the date an edit records the grade from; the first grade a person is given applies from
 *  the beginning of time. */
public record PersonRequest(String name, String grade, Long version, String gradeFrom) {
}

package net.officefloor.hq.app.person;

/** A person's fields, the signed-in user (`by`) making the change, — for an edit — the version
 *  of the person the form loaded (null on an add), and the date the grade is given from (blank or null: the sheet's
 *  today; an add's grade applies from the beginning of time). */
public record PersonRequest(String name, String grade, String by, Long version, String gradeFrom) {
}

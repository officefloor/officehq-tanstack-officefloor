package net.officefloor.hq.app.person;

/** Body of a grade saved from a person's row: who is saving it, the grade, and the version of the person the row
 *  showed (null: not checked). The grade applies from the sheet's today. */
public record InlineGradeRequest(String user, String grade, Long version) {
}

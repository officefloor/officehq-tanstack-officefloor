package net.officefloor.hq.app.person;

/** Body of a row's grade save: the grade chosen, and who saves it. */
public record InlineGradeRequest(String grade, String by) {
}

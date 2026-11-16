package net.officefloor.hq.app.person;

/** A row's grade save once validated: the grade, and who saves it. */
public record InlineGradeValues(String grade, String by) {
}

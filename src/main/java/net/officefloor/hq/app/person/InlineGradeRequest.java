package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people/{person}/grade: the grade the row's select chose. */
public record InlineGradeRequest(String grade) {
}

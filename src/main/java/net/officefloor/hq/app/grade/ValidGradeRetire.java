package net.officefloor.hq.app.grade;

/** A grade retirement that passed validation: a known grade not yet retired on the sheet. */
public record ValidGradeRetire(String grade, String user) {
}

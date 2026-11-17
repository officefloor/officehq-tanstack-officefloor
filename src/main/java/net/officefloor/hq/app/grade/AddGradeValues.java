package net.officefloor.hq.app.grade;

/** A grade's addition once validated: the code, one to three capital letters, and who adds it. */
public record AddGradeValues(String grade, String by) {
}

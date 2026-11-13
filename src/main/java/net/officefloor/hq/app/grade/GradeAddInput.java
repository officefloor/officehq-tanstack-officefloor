package net.officefloor.hq.app.grade;

/** A grade to add, validated, and the user adding it. */
public record GradeAddInput(String grade, String user) {
}

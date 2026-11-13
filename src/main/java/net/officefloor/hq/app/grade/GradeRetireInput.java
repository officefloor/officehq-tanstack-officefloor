package net.officefloor.hq.app.grade;

/** A grade to retire, validated, and the user retiring it. */
public record GradeRetireInput(String grade, String user) {
}

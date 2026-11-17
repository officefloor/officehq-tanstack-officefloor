package net.officefloor.hq.app.grade;

/** A grade merge once validated: two different grades, each one of A, B or C, and who merges them. */
public record MergeGradeValues(String from, String into, String by) {
}

package net.officefloor.hq.app.grade;

/** Body of a grade merge: the grade merged, the grade it is merged into, and who merges it. */
public record MergeGradeRequest(String from, String into, String by) {
}

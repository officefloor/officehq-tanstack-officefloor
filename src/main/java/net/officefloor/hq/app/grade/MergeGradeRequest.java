package net.officefloor.hq.app.grade;

/** Body of merging a grade: the grade merged and the grade its people are given. */
public record MergeGradeRequest(String from, String into) {
}

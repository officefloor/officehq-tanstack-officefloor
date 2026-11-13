package net.officefloor.hq.app.grade;

/** A grade merge, validated: the grade merged, the grade it is merged into and the steward merging. */
public record GradeMergeInput(String from, String into, String user) {
}

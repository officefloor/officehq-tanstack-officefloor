package net.officefloor.hq.app.grade;

/** Body of a grade merge: the grade merged, the grade it is merged into and who merges them. */
public record GradeMergeRequest(String from, String into, String user) {
}

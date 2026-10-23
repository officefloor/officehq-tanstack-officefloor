package net.officefloor.hq.app.grade;

/** Body of a grade merge: who is merging, the grade merged and the grade its people are given. */
public record GradeMergeRequest(String user, String from, String into) {
}

package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/grades/merge: the grade merged, and the grade its people are given. */
public record GradeMergeRequest(String from, String into) {
}

package net.officefloor.hq.app.grade;

/** Body of POST /api/sheets/{sheetId}/grades/merge: the grade merged, the grade it is merged into and the user merging. */
public record GradeMergeRequest(String from, String into, String user) {
}

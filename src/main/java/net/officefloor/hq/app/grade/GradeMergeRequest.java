package net.officefloor.hq.app.grade;

/** The grade to merge and the grade to merge it into, as submitted. */
public record GradeMergeRequest(String from, String into) {
}

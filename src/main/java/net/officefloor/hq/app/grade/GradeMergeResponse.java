package net.officefloor.hq.app.grade;

/** A grade merge done: the grade merged, the grade its people were given and how many were given it. */
public record GradeMergeResponse(String from, String into, int changed) {
}

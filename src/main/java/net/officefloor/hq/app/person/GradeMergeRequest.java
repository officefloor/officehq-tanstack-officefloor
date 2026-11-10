package net.officefloor.hq.app.person;

/** The grade merge: the grade merged away and the grade its people are given (each A, B or C). */
public record GradeMergeRequest(String from, String into) {
}

package net.officefloor.hq.app.person;

/** The grade merge: the grade merged away and the grade its people are given (each one of the sheet's grades). */
public record GradeMergeRequest(String from, String into) {
}

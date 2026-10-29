package net.officefloor.hq.app.grade;

/** The grade merged ({@code from}) and the grade its people are given instead ({@code into}). */
public record MergeRequest(String from, String into) {
}

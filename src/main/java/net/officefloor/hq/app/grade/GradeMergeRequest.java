package net.officefloor.hq.app.grade;

/** A grade merge as the screen sends it: the grade merged and the grade it is merged into. */
public record GradeMergeRequest(String from, String into) {
}

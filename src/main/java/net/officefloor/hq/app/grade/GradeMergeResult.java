package net.officefloor.hq.app.grade;

/** A grade merge done: the grades and how many people it moved. */
public record GradeMergeResult(String from, String into, int merged) {
}

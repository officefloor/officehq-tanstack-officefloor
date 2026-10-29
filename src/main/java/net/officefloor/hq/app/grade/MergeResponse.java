package net.officefloor.hq.app.grade;

/** A merge done: the grades and how many people were given the other grade. */
public record MergeResponse(String from, String into, int people) {
}

package net.officefloor.hq.app.grade;

/** A grade merge that passed validation: two different known grades, by a steward. */
public record ValidGradeMerge(String from, String into, String user) {
}

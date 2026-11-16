package net.officefloor.hq.app.person;

/** The outcome of an import of people: how many were added, and how many already on the sheet were updated. */
public record ImportPeopleResult(int added, int updated) {
}

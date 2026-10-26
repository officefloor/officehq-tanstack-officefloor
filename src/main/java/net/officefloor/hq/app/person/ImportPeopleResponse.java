package net.officefloor.hq.app.person;

/** How many people the import added, and how many existing people it updated. */
public record ImportPeopleResponse(int added, int updated) {
}

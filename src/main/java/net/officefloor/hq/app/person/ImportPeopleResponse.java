package net.officefloor.hq.app.person;

/** How many people the import added, and how many already on the sheet it updated. */
public record ImportPeopleResponse(int added, int updated) {
}

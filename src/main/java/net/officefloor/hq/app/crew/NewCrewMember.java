package net.officefloor.hq.app.crew;

/** A member being added to a crew. */
public record NewCrewMember(Crew crew, CrewPerson member) {
}

package net.officefloor.hq.app.person;

import java.util.List;

/** The people on a sheet, in the order they were added. */
public record PeopleResponse(List<PersonResponse> people) {
}

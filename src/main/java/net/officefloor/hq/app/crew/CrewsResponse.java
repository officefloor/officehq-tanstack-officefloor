package net.officefloor.hq.app.crew;

import java.util.List;

/** The crews on a sheet, by name. */
public record CrewsResponse(List<CrewResponse> crews) {
}

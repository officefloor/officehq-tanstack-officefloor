package net.officefloor.hq.app.crew;

import jakarta.validation.constraints.Size;

/** A crew as the form submits it: its name and its lead's person id. */
public record CrewRequest(@Size(max = 200) String name, Long leadId) {
}

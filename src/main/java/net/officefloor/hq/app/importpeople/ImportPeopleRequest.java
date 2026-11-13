package net.officefloor.hq.app.importpeople;

import jakarta.validation.constraints.Size;

/** The import box's text: one person per line, as name,grade,terms,contact. */
public record ImportPeopleRequest(@Size(max = 200000) String text) {
}

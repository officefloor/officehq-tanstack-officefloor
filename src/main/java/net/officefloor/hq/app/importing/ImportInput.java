package net.officefloor.hq.app.importing;

import java.util.List;
import net.officefloor.hq.app.person.PersonRequest;

/** A validated people import: one person form per line, in order. */
public record ImportInput(List<PersonRequest> people) {
}

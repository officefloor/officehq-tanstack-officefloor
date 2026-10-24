package net.officefloor.hq.app.importpeople;

import java.util.List;
import net.officefloor.hq.app.person.PersonRequest;

/** The import box's lines, each checked by the person form's rules, in order. */
public record ImportedLines(List<PersonRequest> people) {
}

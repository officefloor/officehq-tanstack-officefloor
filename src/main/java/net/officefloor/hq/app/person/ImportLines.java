package net.officefloor.hq.app.person;

import java.util.List;

/** The imported lines, in order, each checked as the person form checks it. */
public record ImportLines(List<PersonRequest> people) {
}

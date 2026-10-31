package net.officefloor.hq.app.person;

import java.util.List;

/** The checked lines of a people import, in order, each as the person form would send it, and who adds them. */
public record ImportedPeople(List<PersonRequest> lines, String user) {
}

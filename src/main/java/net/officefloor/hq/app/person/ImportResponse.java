package net.officefloor.hq.app.person;

import java.util.List;

/** The outcome of an import: the counts added and updated, or the (1-based) numbers of the lines refused. */
public record ImportResponse(int added, int updated, List<Integer> errors) {
}

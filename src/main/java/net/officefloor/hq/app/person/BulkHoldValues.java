package net.officefloor.hq.app.person;

import java.util.List;

/** A bulk hold once validated: the selected people (each once) and the hold they are each given. */
public record BulkHoldValues(List<Long> personIds, Hold hold) {
}

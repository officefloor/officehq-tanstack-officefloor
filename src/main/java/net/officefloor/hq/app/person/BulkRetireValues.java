package net.officefloor.hq.app.person;

import java.util.List;

/** A bulk retire once validated: the selected people (each once) and the retirement they are each given. */
public record BulkRetireValues(List<Long> personIds, Retirement retirement) {
}

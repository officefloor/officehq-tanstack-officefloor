package net.officefloor.hq.app.claim;

import java.util.List;

/** The body of a bulk open: the ids of the selected claims. */
public record BulkOpenRequest(List<Long> ids) {
}

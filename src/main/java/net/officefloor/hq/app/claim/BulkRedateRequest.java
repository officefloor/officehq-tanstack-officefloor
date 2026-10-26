package net.officefloor.hq.app.claim;

import java.util.List;

/** The selected claims' ids to re-date together, their new date (yyyy-mm-dd) and the signed-in user (`by`). */
public record BulkRedateRequest(List<Long> ids, String date, String by) {
}

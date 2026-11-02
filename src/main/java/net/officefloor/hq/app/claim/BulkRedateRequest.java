package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheet}/claims/bulk-redate: the ids of the selected claims and their new date (yyyy-MM-dd). */
public record BulkRedateRequest(List<Long> claims, String date) {
}

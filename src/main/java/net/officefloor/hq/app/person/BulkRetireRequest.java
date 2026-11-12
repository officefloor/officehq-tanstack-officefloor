package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk retire, as sent: the ids of the selected people, the date (yyyy-MM-dd, blank for the
 *  sheet's today) they are retired from and who retires them. */
public record BulkRetireRequest(List<String> personIds, String date, String user) {
}

package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk hold, as sent: the ids of the selected people, the from-date (blank for the sheet's
 *  today), the to-date (blank for no end) and who puts them on hold. */
public record BulkHoldRequest(List<String> personIds, String from, String to, String user) {
}

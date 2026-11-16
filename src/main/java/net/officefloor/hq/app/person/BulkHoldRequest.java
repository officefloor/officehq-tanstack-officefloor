package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk hold: the ids of the selected people, in any order, the hold's from-date (blank:
 *  the sheet's today) and its to-date (blank: none). */
public record BulkHoldRequest(List<Long> personIds, String from, String to) {
}

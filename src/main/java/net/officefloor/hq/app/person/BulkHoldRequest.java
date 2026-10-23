package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk hold: who is holding, the ids of the selected people, the date the hold runs from (yyyy-mm-dd;
 *  null or blank: the sheet's today) and the date it runs to (null or blank: open-ended). */
public record BulkHoldRequest(String user, List<Long> personIds, String from, String to) {
}

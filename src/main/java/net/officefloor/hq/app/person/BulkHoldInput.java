package net.officefloor.hq.app.person;

import java.util.List;

/** A validated bulk hold: the selected person ids, each once, the hold (its from-date already
 *  defaulted to the sheet's today) and who puts it. */
public record BulkHoldInput(List<Long> personIds, Hold hold, String user) {
}

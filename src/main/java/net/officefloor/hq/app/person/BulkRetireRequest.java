package net.officefloor.hq.app.person;

import java.util.List;

/** The selected people's ids to retire together, the date to retire them from (blank or null: the sheet's today),
 *  and the signed-in user (`by`) retiring them. */
public record BulkRetireRequest(List<Long> ids, String date, String by) {
}

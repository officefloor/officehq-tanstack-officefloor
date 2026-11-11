package net.officefloor.hq.app.person;

import java.util.List;

/**
 * A bulk retirement as the screen sends it: the ids of the selected people and the date they are
 * retired from (yyyy-MM-dd; blank for the sheet's today).
 */
public record BulkRetireRequest(List<String> personIds, String from) {
}

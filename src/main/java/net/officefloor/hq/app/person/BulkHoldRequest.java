package net.officefloor.hq.app.person;

import java.util.List;

/**
 * A bulk hold as the screen sends it: the ids of the selected people, the hold's from-date
 * (yyyy-MM-dd; blank for the sheet's today) and its to-date (blank: none).
 */
public record BulkHoldRequest(List<String> personIds, String from, String to) {
}

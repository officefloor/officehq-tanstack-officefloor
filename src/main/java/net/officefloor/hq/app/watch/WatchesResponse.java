package net.officefloor.hq.app.watch;

import java.util.List;

/** The ids of the people the signed-in user watches on the sheet. */
public record WatchesResponse(List<String> personIds) {
}

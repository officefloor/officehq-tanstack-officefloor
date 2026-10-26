package net.officefloor.hq.app.session;

import java.util.List;

/** The people-list columns the user has switched off on the sheet. */
public record ColumnsResponse(String user, List<String> hidden) {
}

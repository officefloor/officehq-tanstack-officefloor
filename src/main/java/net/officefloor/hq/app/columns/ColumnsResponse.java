package net.officefloor.hq.app.columns;

import java.util.List;

/** The people-list columns the signed-in user has switched off. */
public record ColumnsResponse(List<String> hidden) {
}

package net.officefloor.hq.app.columns;

import java.util.List;

/** The people-list columns the signed-in user switched off on the sheet. */
public record UserColumns(List<String> hidden) {
}

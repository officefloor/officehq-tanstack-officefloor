package net.officefloor.hq.app.columns;

import java.util.List;

/** The people-list columns a user has switched off on their screen. */
public record UserColumns(List<String> hidden) {
}

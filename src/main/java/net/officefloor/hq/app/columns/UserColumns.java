package net.officefloor.hq.app.columns;

import java.util.List;

/** The columns of the people list {@code user} has switched off on the sheet. */
public record UserColumns(String user, List<String> hidden) {
}

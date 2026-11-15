package net.officefloor.hq.app.user;

import java.util.List;

/** The columns of the people list a user has switched off on a sheet. */
public record UserColumns(String user, String sheetId, List<String> hidden) {
}

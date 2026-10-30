package net.officefloor.hq.app.sheet;

import java.util.List;

/** The other sheets of the sheet's tree, by sheet id. */
public record TreeResponse(List<String> sheets) {
}

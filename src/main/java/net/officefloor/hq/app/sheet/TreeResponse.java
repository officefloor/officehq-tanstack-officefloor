package net.officefloor.hq.app.sheet;

import java.util.List;

/** The other sheets of a sheet's tree: its ancestors and descendants, each once, in id order. */
public record TreeResponse(List<String> sheets) {
}

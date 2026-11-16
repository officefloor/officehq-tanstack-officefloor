package net.officefloor.hq.app.claim;

import java.util.List;

/** The pasted lines in order, and who pastes them. */
public record PastedLines(List<PastedLine> lines, String by) {
}

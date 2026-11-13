package net.officefloor.hq.app.paste;

import java.util.List;

/** The pasted lines, in the order they were typed. */
public record PastedLines(List<PastedLine> lines) {
}

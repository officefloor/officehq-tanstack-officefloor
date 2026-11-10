package net.officefloor.hq.app.paste;

import java.util.List;

/** The pasted lines, in the order they were pasted. */
public record PasteLines(List<PasteLine> lines) {
}

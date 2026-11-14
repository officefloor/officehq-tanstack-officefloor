package net.officefloor.hq.app.paste;

import java.util.List;

/** The pasted lines, in order, each checked as the claim form checks it. */
public record PasteLines(List<PasteLine> lines) {
}

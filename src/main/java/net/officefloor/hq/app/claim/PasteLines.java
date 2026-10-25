package net.officefloor.hq.app.claim;

import java.util.List;

/** The pasted lines, in order, each checked under the claim form's rules. */
public record PasteLines(List<PasteLine> lines) {
}

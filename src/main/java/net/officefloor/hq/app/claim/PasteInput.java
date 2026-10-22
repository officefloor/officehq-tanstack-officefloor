package net.officefloor.hq.app.claim;

import java.util.List;

/** A validated paste: who is making it and its lines, in the order they were pasted. */
public record PasteInput(String user, List<PasteLine> lines) {
}

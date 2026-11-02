package net.officefloor.hq.app.paste;

import java.util.List;

/** A validated paste: its lines, in order. */
public record PasteInput(List<PasteLine> lines) {
}

package net.officefloor.hq.app.paste;

import java.util.List;

/** A validated paste: its claims in the order pasted. */
public record PasteInput(List<PastedClaim> claims) {
}

package net.officefloor.hq.app.paste;

import java.util.List;

/** Every pasted line, checked, in the order pasted. */
public record PastedClaims(List<PastedClaim> lines) {
}

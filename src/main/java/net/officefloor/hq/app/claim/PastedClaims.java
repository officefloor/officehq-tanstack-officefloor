package net.officefloor.hq.app.claim;

import java.util.List;

/** The checked lines of a paste, in order, and who adds them. */
public record PastedClaims(List<PastedLine> lines, String user) {
}

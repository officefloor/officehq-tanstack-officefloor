package net.officefloor.hq.app.claim;

import java.util.List;

/** The pasted claims in order, each checked and ready to add. */
public record PastedClaims(List<PastedClaim> claims) {
}

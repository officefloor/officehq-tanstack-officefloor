package net.officefloor.hq.app.claim;

import java.util.List;

/** A sheet's claims, newest first. */
public record Claims(List<Claim> claims) {
}

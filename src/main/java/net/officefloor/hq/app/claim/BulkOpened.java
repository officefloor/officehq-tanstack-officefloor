package net.officefloor.hq.app.claim;

import java.util.List;

/** The claims a bulk open opened, in the order they were numbered, and how many it refused. */
public record BulkOpened(List<Claim> opened, int refused) {
}

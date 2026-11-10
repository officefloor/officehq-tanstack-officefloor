package net.officefloor.hq.app.pool;

import java.util.List;

/** The pools on a sheet, by name. */
public record PoolsResponse(List<PoolResponse> pools) {
}

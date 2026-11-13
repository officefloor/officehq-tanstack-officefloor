package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool on a sheet: its name and its members, in the order they were added. {@code id} is null until saved. */
public record Pool(Long id, String sheetId, String name, List<PoolMember> members) {
}

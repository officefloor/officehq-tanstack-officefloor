package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.List;

/** The sheet's pools, in the order they were added, and the sheet's pot (null: none) shared out among each balanced one's members. */
public record Pools(List<Pool> list, BigDecimal pot) {
}

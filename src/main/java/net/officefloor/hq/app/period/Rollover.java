package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A year end rollover: the old fiscal year's name (FY<yyyy>), the new year's first period (null when there is none),
 * and each person's open figure at the end of the old year, by person id in sheet order.
 */
public record Rollover(String fy, Period first, Map<Long, BigDecimal> openings) {
}

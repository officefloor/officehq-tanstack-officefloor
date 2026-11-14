package net.officefloor.hq.app.rollover;

import java.time.YearMonth;
import java.util.List;
import net.officefloor.hq.app.period.Period;

/** A rollover under way: the old fiscal year's name (FY<yyyy>), its periods, earliest first, and the new year's first month. */
public record Rollover(String fy, List<Period> oldPeriods, YearMonth newFirst) {

    /** The old year's last period: the one ending latest. */
    public Period last() {
        return oldPeriods.get(oldPeriods.size() - 1);
    }
}

package net.officefloor.hq.app.rollover;

import java.util.List;
import net.officefloor.hq.app.period.Period;

/** The old fiscal year being rolled over: its name (FY<yyyy>) and its periods on the sheet, earliest first. */
public record OldYear(String name, List<Period> periods) {

    /** The old year's last period: the one ending latest. */
    public Period last() {
        return periods.stream().reduce((a, b) -> b.to().isAfter(a.to()) ? b : a).orElseThrow();
    }
}

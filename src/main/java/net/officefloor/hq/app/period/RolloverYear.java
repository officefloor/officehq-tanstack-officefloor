package net.officefloor.hq.app.period;

import java.util.List;

/** The fiscal year being rolled over (the yyyy of FY<yyyy>), its periods by from-date, and who rolls it. */
public record RolloverYear(int year, List<Period> periods, String user) {
}

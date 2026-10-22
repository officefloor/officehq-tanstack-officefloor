package net.officefloor.hq.app.period;

import java.time.YearMonth;

/** A validated period generate: who is making it, and the first and last month (first on or before last). */
public record PeriodGenerateInput(String user, YearMonth first, YearMonth last) {
}

package net.officefloor.hq.app.period;

import java.time.YearMonth;

/** A validated generate: the first and last months to make a period for and who makes them. */
public record GenerateInput(YearMonth from, YearMonth to, String user) {
}

package net.officefloor.hq.app.rate;

import java.time.LocalDate;

/**
 * A rate whose newest version was withdrawn: its grade and from-date (null: from the start), and the version now in
 * force (0: none, so the rate is gone).
 */
public record RateWithdrawn(String sheetId, String grade, LocalDate from, int version) {
}

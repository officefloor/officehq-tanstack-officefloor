package net.officefloor.hq.app.history;

import java.time.LocalDate;

/** The date the signed-in user's screen shows the sheet as of; null {@code on}: the sheet as it stands now. */
public record ViewAsOf(LocalDate on) {
}

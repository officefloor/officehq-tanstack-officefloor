package net.officefloor.hq.app.flag;

import java.time.LocalDate;

/** A validated flag: the signed-in user, the trimmed reason and the date it runs until. */
public record FlagInput(String user, String reason, LocalDate until) {
}

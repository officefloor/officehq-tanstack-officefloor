package net.officefloor.hq.app.access;

import java.time.LocalDate;

/** A validated delegation: the trimmed user name, its from-date (null: the sheet's today) and its to-date. */
public record DelegateInput(String user, LocalDate from, LocalDate to) {
}

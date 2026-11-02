package net.officefloor.hq.app.settings;

import java.time.LocalDate;

/** A validated delegate form; {@code from} is null when left blank (the sheet's today). */
public record DelegationInput(String user, LocalDate from, LocalDate to) {
}

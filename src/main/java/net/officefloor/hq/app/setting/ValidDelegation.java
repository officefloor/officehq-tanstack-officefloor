package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** A delegation that passed validation: the user given the steward role, from and to (inclusive), by a steward. */
public record ValidDelegation(String name, LocalDate from, LocalDate to, String user) {
}

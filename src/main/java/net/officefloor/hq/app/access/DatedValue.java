package net.officefloor.hq.app.access;

import java.time.LocalDate;

/** A dated setting's value (null: blank), in force from its from-date until the next one's. */
public record DatedValue(String key, LocalDate from, String value) {
}

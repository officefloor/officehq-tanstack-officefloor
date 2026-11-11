package net.officefloor.hq.app.rate;

import java.time.LocalDate;

/** A rate's grade and from-date (null: from the beginning of time), whose newest version is to be, or was, withdrawn. */
public record WithdrawnRate(String grade, LocalDate from) {
}

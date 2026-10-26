package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** Own terms a person was given (null: none), in force from their from-date. */
public record TermsFrom(Integer terms, LocalDate from) {
}

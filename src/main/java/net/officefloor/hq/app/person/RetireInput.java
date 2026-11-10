package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated retirement: the date the person is retired from (null: the sheet's today). */
public record RetireInput(LocalDate from) {
}

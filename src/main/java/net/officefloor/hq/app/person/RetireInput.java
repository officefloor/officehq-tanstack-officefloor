package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated retirement; a null date means the sheet's today. */
public record RetireInput(LocalDate date) {
}

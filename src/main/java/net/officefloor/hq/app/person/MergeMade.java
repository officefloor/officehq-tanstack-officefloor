package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A merge made into a person: its id, the person merged (their id and name), who made it and on which sheet-today. */
public record MergeMade(long id, long personId, String name, String by, LocalDate madeOn) {
}

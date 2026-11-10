package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** One statement taken for a person: its due-total (n/a when it had none) and the date it was taken. */
public record StatedFigure(String dueTotal, LocalDate date) {
}

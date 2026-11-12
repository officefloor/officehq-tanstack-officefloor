package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated unretirement: the day unretired (the sheet's today) and who unretires them. */
public record UnretireInput(LocalDate on, String user) {
}

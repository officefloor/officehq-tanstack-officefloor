package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** An unretirement: by whom, and the day (the sheet's today) once it is made. */
public record Unretirement(String user, LocalDate on) {
}

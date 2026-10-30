package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A bulk retire that passed validation. A null date takes the sheet's today. */
public record ValidBulkRetire(List<String> personIds, LocalDate date, String user) {
}

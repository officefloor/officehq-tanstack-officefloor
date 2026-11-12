package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A validated bulk retire: the selected person ids, each once, the date they are retired from and who
 *  retires them. */
public record BulkRetireInput(List<Long> personIds, LocalDate from, String user) {
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A merge of a person (`fromId`) into another (`intoId`), by whom and on which sheet-today. */
public record PersonMerge(long id, long fromId, long intoId, String user, LocalDate on) {
}

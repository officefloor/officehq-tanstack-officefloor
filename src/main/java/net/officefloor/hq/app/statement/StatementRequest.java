package net.officefloor.hq.app.statement;

import jakarta.validation.constraints.Size;

/** A statement to take: as of a date (yyyy-MM-dd), or blank for now. */
public record StatementRequest(@Size(max = 20) String asof) {
}

package net.officefloor.hq.app.patron;

import jakarta.validation.constraints.NotNull;

/** The person the row names as a patron, by id. */
public record PatronRequest(@NotNull Long patronId) {
}

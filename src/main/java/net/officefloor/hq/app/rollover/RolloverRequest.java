package net.officefloor.hq.app.rollover;

import jakarta.validation.constraints.Size;

/** A year-end rollover as the form submits it: the old fiscal year (yyyy) to roll over from. */
public record RolloverRequest(@Size(max = 30) String year) {
}

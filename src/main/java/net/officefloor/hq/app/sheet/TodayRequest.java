package net.officefloor.hq.app.sheet;

import jakarta.validation.constraints.Size;

/** A new today for a sheet, as yyyy-MM-dd; blank resets it to the real date. */
public record TodayRequest(@Size(max = 20) String today) {
}

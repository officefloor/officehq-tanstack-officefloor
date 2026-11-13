package net.officefloor.hq.app.period;

import jakarta.validation.constraints.Size;

/** A period as the form submits it: the name (required) and the from and to dates (yyyy-MM-dd, required). */
public record PeriodRequest(@Size(max = 100) String name, @Size(max = 30) String from, @Size(max = 30) String to) {
}

package net.officefloor.hq.app.period;

import jakarta.validation.constraints.Size;

/**
 * A period as the form submits it: the name (required), the from and to dates (yyyy-MM-dd, required) and the cap on
 * each person's open claims in it (a whole number of at least 1; blank or missing: none).
 */
public record PeriodRequest(@Size(max = 100) String name, @Size(max = 30) String from, @Size(max = 30) String to, @Size(max = 30) String cap) {
}

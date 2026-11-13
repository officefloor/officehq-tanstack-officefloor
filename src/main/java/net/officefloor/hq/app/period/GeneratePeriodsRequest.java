package net.officefloor.hq.app.period;

import jakarta.validation.constraints.Size;

/** A generate of monthly periods as the form submits it: the from and to dates (yyyy-MM-dd; blank is the sheet's today). */
public record GeneratePeriodsRequest(@Size(max = 30) String from, @Size(max = 30) String to) {
}

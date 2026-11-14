package net.officefloor.hq.app.period;

import jakarta.validation.constraints.Size;

/** A generate of a fiscal year's monthly periods as the form submits it: the year (yyyy) its first day falls in. */
public record GenerateFiscalYearRequest(@Size(max = 30) String year) {
}

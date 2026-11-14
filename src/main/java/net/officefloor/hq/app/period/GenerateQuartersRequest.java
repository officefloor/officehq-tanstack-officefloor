package net.officefloor.hq.app.period;

import jakarta.validation.constraints.Size;

/** A generate of a year's quarterly periods as the form submits it: the calendar year (yyyy). */
public record GenerateQuartersRequest(@Size(max = 30) String year) {
}

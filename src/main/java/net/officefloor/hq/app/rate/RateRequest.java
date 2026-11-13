package net.officefloor.hq.app.rate;

import jakarta.validation.constraints.Size;

/** A rate as the form submits it: the grade (A, B or C) and the percent (required, at most two decimals). */
public record RateRequest(@Size(max = 10) String grade, @Size(max = 30) String rate) {
}

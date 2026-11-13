package net.officefloor.hq.app.rate;

import jakarta.validation.constraints.Size;

/**
 * A rate as the form submits it: the grade (A, B or C), the percent (required, at most two decimals)
 * and the from-date (yyyy-MM-dd; blank: from the beginning of time).
 */
public record RateRequest(@Size(max = 10) String grade, @Size(max = 30) String rate, @Size(max = 30) String from) {
}

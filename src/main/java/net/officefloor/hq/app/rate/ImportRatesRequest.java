package net.officefloor.hq.app.rate;

import jakarta.validation.constraints.Size;

/** The rate import box's text: one rate per line, as grade,rate,from,to. */
public record ImportRatesRequest(@Size(max = 200000) String text) {
}

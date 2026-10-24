package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A validated rate preview: the grade and the candidate percent with two decimals. */
public record RatePreviewInput(String grade, BigDecimal pct) {
}

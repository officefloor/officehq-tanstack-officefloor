package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A validated rate preview: its grade and its candidate percent scaled to two decimals. */
public record PreviewInput(String grade, BigDecimal pct) {
}

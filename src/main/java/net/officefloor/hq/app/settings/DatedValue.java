package net.officefloor.hq.app.settings;

/** A value a dated setting has from a date (yyyy-MM-dd); null when blank. */
public record DatedValue(String from, String value) {
}

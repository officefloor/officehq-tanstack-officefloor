package net.officefloor.hq.app.setting;

/** A dated setting's value as recorded: in force from its from-date (yyyy-MM-dd); "" where its key allows a blank. */
public record DatedValue(String from, String value) {
}

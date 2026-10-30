package net.officefloor.hq.app.setting;

/** A value recorded for a dated setting: the date it is in force from and the value ("" for none). */
public record DatedValue(String from, String value) {
}

package net.officefloor.hq.app.template;

/** A claim template on the sheet: its name, its amount (two decimals) and its note. */
public record ClaimTemplate(String name, String amount, String note) {
}

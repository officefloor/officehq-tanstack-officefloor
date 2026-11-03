package net.officefloor.hq.app.template;

/** A sheet's claim template: its name, its amount (two decimals) and its note. */
public record Template(String name, String amount, String note) {
}

package net.officefloor.hq.app.locale;

/** The signed-in user's locale (en or de); also the body of POST /api/sheets/{sheet}/locale. */
public record UserLocale(String locale) {
}

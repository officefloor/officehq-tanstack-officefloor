package net.officefloor.hq.app.locale;

/** Body of a locale choice: the signed-in user and the locale they choose ('en' or 'de'). */
public record LocaleRequest(String user, String locale) {
}

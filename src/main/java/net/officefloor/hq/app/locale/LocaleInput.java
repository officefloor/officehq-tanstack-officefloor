package net.officefloor.hq.app.locale;

/** A validated locale choice: the signed-in user and one of the locales offered. */
public record LocaleInput(String user, String locale) {
}

package net.officefloor.hq.app.person;

/** Whether the terms an edit entered differ from the own terms in force on the sheet's today before the save. */
public record TermsEdit(boolean changed) {
}

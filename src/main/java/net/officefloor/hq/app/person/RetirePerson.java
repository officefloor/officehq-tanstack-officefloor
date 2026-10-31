package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the date given, or the sheet's today. Not an edit: the person's
 *  version is untouched, so an editor open on them can still save. */
public class RetirePerson {
    public void retire(@Val Retirement retirement, @Val Person person, Sheets sheets, People people) {
        person.setRetiredFrom(retirement.from() != null ? retirement.from() : sheets.today(person.getSheetId()));
        people.saveRetirement(person);
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses the quick-add when the named person is retired before the sheet's today (the claim's date). */
public class CheckQuickClaimRetired {
    public void check(@Val Person person, SheetRepository sheets) throws RetiredPersonException {
        if (person.getRetiredFrom() == null) {
            return;
        }
        var today = sheets.findById(person.getSheetId()).orElseThrow().getToday();
        if (today.isAfter(person.getRetiredFrom())) {
            throw new RetiredPersonException(person.getName() + " is retired from " + person.getRetiredFrom()
                    + "; a claim cannot be dated after that.");
        }
    }
}

package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses a quick-add for a person retired before the sheet's today, the claim's date, before anything is written. */
public class CheckQuickAddRetired {
    public void check(@Val Sheet sheet, @Val Person person) throws RetiredPersonException {
        if (person.retiredBefore(sheet.today())) throw new RetiredPersonException(person.getName());
    }
}

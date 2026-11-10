package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses a quick-add for a person retired before the claim's date (the sheet's entry date while it is set, else its today) before anything is written. */
public class CheckQuickAddRetired {
    public void check(@Val Sheet sheet, @Val Person person, SettingsRepository settings) throws RetiredPersonException {
        if (person.retiredBefore(settings.find(sheet.id()).entryDay(sheet.today()))) throw new RetiredPersonException(person.getName());
    }
}

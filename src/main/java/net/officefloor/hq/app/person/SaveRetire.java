package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Retire the loaded person from the date given, or the sheet's today when none was. */
public class SaveRetire {
    public void save(@Val RetireInput input, @Val Person loaded, SheetRepository sheets, PersonRepository repository,
            Out<PersonResponse> saved) {
        var from = input.from() != null ? input.from() : sheets.today(loaded.sheetId());
        repository.retire(loaded, from);
        saved.set(PersonResponse.from(new Person(loaded.id(), loaded.sheetId(), loaded.name(), loaded.grade(),
                loaded.version(), loaded.statementCount(), loaded.statedTotal(), loaded.statementDate(), from, loaded.grades(), loaded.holds(), loaded.terms(), loaded.contact(), loaded.aliases(), loaded.origin(), loaded.retirements(), loaded.tags(), loaded.statements())));
    }
}

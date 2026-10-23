package net.officefloor.hq.app.person;

import java.util.ArrayList;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Unretire the loaded person on the sheet's today, keeping the retirement as a past one. */
public class SaveUnretire {
    public void save(@Val Person loaded, SheetRepository sheets, PersonRepository repository,
            Out<PersonResponse> saved) {
        var on = sheets.today(loaded.sheetId());
        repository.unretire(loaded, on);
        var retirements = new ArrayList<>(loaded.retirements());
        retirements.add(new Person.Retirement(loaded.retiredFrom(), on));
        saved.set(PersonResponse.from(new Person(loaded.id(), loaded.sheetId(), loaded.name(), loaded.grade(),
                loaded.version(), loaded.statementCount(), loaded.statedTotal(), loaded.statementDate(), null,
                loaded.grades(), loaded.holds(), loaded.terms(), loaded.contact(), loaded.aliases(), loaded.origin(),
                retirements, loaded.tags())));
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Put the loaded person on hold when they are not, else take them off it. */
public class SaveHold {
    public void save(@Val Person loaded, PersonRepository repository, Out<PersonResponse> saved) {
        boolean held = !loaded.held();
        repository.hold(loaded, held);
        saved.set(PersonResponse.from(new Person(loaded.id(), loaded.sheetId(), loaded.name(), loaded.grade(),
                loaded.version(), loaded.statementCount(), loaded.statedTotal(), loaded.statementDate(),
                loaded.retiredFrom(), held)));
    }
}

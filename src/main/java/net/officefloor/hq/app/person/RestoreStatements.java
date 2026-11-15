package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** A transfer undone: the person, back on the sheet they came from, has the statements the transfer
 *  dropped from them there again. */
public class RestoreStatements {
    public void restore(@Val Transfer transfer, PersonRepository people) {
        people.restoreStatements(transfer.to(), transfer.person().getId());
    }
}

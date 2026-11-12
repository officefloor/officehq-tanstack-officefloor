package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class RetirePerson {
    public void retire(@Val RetireInput input, @Val Person person, PersonStore store) {
        store.retire(person, input.from());
    }
}

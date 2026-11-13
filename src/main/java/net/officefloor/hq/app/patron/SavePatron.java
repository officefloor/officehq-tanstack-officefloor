package net.officefloor.hq.app.patron;

import net.officefloor.plugin.variable.Val;

public class SavePatron {
    public void save(@Val Patron patron, PatronRepository patrons) {
        patrons.save(patron);
    }
}

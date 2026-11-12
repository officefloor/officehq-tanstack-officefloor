package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;

/** Locks or unlocks a period for {@link LockPeriod} and {@link UnlockPeriod}: only a steward may, and only a
 *  period not already so. */
final class PeriodLocking {
    private PeriodLocking() {
    }

    static PeriodResponse set(String sheet, String name, String user, boolean locked, PeriodStore store,
            RoleStore roles) throws InvalidSheetException {
        if (roles.find(sheet, user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward may " + (locked ? "lock" : "unlock") + " a period");
        }
        PeriodResponse period = store.find(sheet, name);
        if (period == null) {
            throw new InvalidSheetException("No such period: " + name);
        }
        if (period.locked() == locked || !store.lock(sheet, period.name(), locked)) {
            throw new InvalidSheetException("The period " + period.name() + " is " + (locked ? "already" : "not")
                    + " locked");
        }
        return store.find(sheet, period.name());
    }
}

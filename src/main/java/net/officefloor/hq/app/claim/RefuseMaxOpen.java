package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse opening a claim whose person already has as many OPEN or PENDING claims (other than this one) as the
 *  sheet's maximum of open claims per person. Shared by a single open, the bulk open and a paste. */
public class RefuseMaxOpen {
    public void refuse(@Val ClaimStatusInput input, @Val Claim loaded, SettingRepository settings, ClaimRepository claims)
            throws ClaimStatusNotAllowedException {
        if (input.action().equals("open")) {
            refuse(loaded, settings, claims);
        }
    }

    /** Throws when the claim's person is already at the sheet's maximum of open claims. */
    static void refuse(Claim claim, SettingRepository settings, ClaimRepository claims)
            throws ClaimStatusNotAllowedException {
        Integer max = settings.maxOpen(claim.sheetId());
        if (max == null) {
            return;
        }
        long open = claims.listForPerson(claim.sheetId(), claim.personId()).stream()
                .filter(c -> c.id() != claim.id() && c.status().unsettled()).count();
        if (open >= max) {
            throw new ClaimStatusNotAllowedException("This person already has " + open
                    + " open claims, the sheet's maximum of " + max + ".");
        }
    }

    /** Whether the claim's person is already at the sheet's maximum of open claims. */
    static boolean reached(Claim claim, SettingRepository settings, ClaimRepository claims) {
        try {
            refuse(claim, settings, claims);
            return false;
        } catch (ClaimStatusNotAllowedException e) {
            return true;
        }
    }
}

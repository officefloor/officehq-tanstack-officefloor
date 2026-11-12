package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Works out the move a status change makes. A clerk settling an OPEN claim whose due is above the
 *  sheet's clerk limit sends it to PENDING instead. A claim whose due is n/a is within any limit; a
 *  sheet with no limit, and a steward, settle anything. Only a steward reviews a PENDING claim, and
 *  only a review moves it to SETTLED or back to OPEN; a review of a claim that is not PENDING is refused. */
public class CheckClerkLimit {
    public void check(@Val StatusInput input, @Val Claim claim, RoleStore roles, SettingStore settings,
            PersonStore people, RateStore rates, SheetStore sheets, ClaimStore claims, Out<StatusMove> move)
            throws InvalidSheetException {
        ClaimStatus from = claim.getStatus();
        boolean steward = roles.find(claim.getSheetId(), input.user()) == Role.STEWARD;
        if (input.review()) {
            if (from != ClaimStatus.PENDING) {
                throw new InvalidSheetException("Only a PENDING claim can be approved or rejected");
            }
            if (!steward) {
                throw new InvalidSheetException("Only a steward can approve or reject a claim");
            }
            move.set(new StatusMove(from, input.status()));
            return;
        }
        if (from == ClaimStatus.PENDING && input.status() != ClaimStatus.VOID) {
            throw new InvalidSheetException("A PENDING claim waits for a steward to approve or reject it");
        }
        move.set(new StatusMove(from, overLimit(input, claim, steward, settings, people, rates, sheets, claims)
                ? ClaimStatus.PENDING : input.status()));
    }

    private static boolean overLimit(StatusInput input, Claim claim, boolean steward, SettingStore settings,
            PersonStore people, RateStore rates, SheetStore sheets, ClaimStore claims) {
        if (input.status() != ClaimStatus.SETTLED || claim.getStatus() != ClaimStatus.OPEN || steward) {
            return false;
        }
        BigDecimal limit = settings.clerkLimit(claim.getSheetId());
        if (limit == null) {
            return false;
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal pct = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
        BigDecimal due = Lateness.of(claim.getSheetId(), settings, sheets, claims).due(claim, pct, settings.floor(claim.getSheetId()));
        return due != null && due.compareTo(limit) > 0;
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Move the loaded claim to its new status, refusing a change its status does not allow (or one
 *  another user's change got to first). Opening gives the claim the sheet's next number; voiding
 *  keeps it (a number is never reused). Opening records who opened it, and that user may not settle it. Settling fixes its due
 *  as its row shows it now: the bound figure, else live at the person's grade rate now in force on the claim's date,
 *  with the late charge as of today on it, at least the sheet's floor in force on its date, frozen in. A claim whose due (as shown) is above the sheet's proof threshold in force
 *  on the claim's date cannot be settled or approved while its proofs count is 0 (refused before the clerk limit is
 *  considered; no threshold, or a due of n/a, is within it). When a clerk (any user not made a steward)
 *  settles a claim whose due (as shown: bound by a statement, else live, plus any late charge) is above the sheet's
 *  clerk limit, it becomes PENDING instead (a due of n/a, or no limit, is within it). A steward other than the
 *  user who opened it approves a PENDING claim (it becomes SETTLED, its due as its row shows it then — live, a
 *  PENDING claim is not bound by a statement — frozen in) or rejects it (OPEN again, bound again by the statement
 *  that bound it). A steward re-settles a SETTLED claim: it is frozen again at the figure it would have today as an
 *  OPEN claim bound by no statement (today's rates, grades, floor and late charge). The new status is added to the claim's status log. The rules are in {@link ClaimStatusChange}. */
public class SaveClaimStatus {
    public void save(@Val ClaimStatusInput input, @Val Claim loaded, ClaimRepository repository,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets, ClaimLogRepository log,
            Out<ClaimResponse> saved)
            throws ClaimStatusNotAllowedException {
        saved.set(ClaimStatusChange.save(input, loaded, repository, people, rates, roles, settings, sheets, log));
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.voidreason.VoidReasonStore;
import net.officefloor.hq.app.watch.WatchStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Part-settles an OPEN claim: splits the amount off it through the very steps of a split, then settles the new
 *  claim by the user through the very steps of a settle (a clerk's settle above the clerk limit sends it to PENDING).
 *  The four-eyes rules and the proof rule are judged on the original claim, before the split; the new claim, opened
 *  by the user, is not judged by them again. All or nothing: a refusal of either rolls both back and drops their
 *  audit records. */
public class ApplyPartSettle {
    public void apply(@Val SplitInput input, @Val Claim claim, ClaimStore store, PersonStore people, PeriodStore periods,
            SettingStore settings, SheetStore sheets, RoleStore roles, RateStore rates, ClaimLogStore log,
            HolidayStore holidays, RollupService rollups, Audit audit, WatchStore watches, VoidReasonStore voidReasons,
            PlatformTransactionManager transactions) throws InvalidSheetException {
        StatusInput status = new StatusInput(ClaimStatus.SETTLED, input.user(), false, "");
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        InvalidSheetException[] refused = new InvalidSheetException[1];
        audit.hold();
        boolean done = false;
        try {
            done = Boolean.TRUE.equals(nested.execute(tx -> {
                try {
                    String sheet = claim.getSheetId();
                    Role role = roles.find(sheet, input.user());
                    // The split, as the split button makes it.
                    new CheckClaimPeriod().check(claim, periods);
                    new CheckSplit().check(input, claim, roles, settings, sheets);
                    // The settle's own rules judged on the original claim.
                    CheckClerkLimit.refuseUnpermitted(ClaimStatus.SETTLED, sheet, role, settings);
                    if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(input.user())) {
                        throw new InvalidSheetException("The user who opened a claim cannot settle it");
                    }
                    if (claim.getEditedBy() != null && claim.getEditedBy().equals(input.user())) {
                        throw new InvalidSheetException("The user who last edited a claim cannot settle it");
                    }
                    CheckProofs.refuseUnproved(status, claim, settings, people, rates, sheets, store);
                    Rollup beforeSplit = rollups.snapshot(sheet);
                    Split[] split = new Split[1];
                    new ApplySplit().apply(input, claim, store, people, settings, sheets, periods, log, holidays,
                            s -> split[0] = s);
                    new AuditClaimSplit().audit(input, claim, split[0], beforeSplit, people, audit, rollups);
                    // The settle of the new claim, as the settle button makes it.
                    Claim into = split[0].into();
                    sheets.refuseArchived(sheet);
                    new CheckClaimPeriod().check(into, periods);
                    StatusMove move = new StatusMove(ClaimStatus.OPEN, CheckClerkLimit.overLimit(status, into,
                            role == Role.STEWARD, settings, people, rates, sheets, store)
                                    ? ClaimStatus.PENDING : ClaimStatus.SETTLED);
                    Rollup beforeSettle = rollups.snapshot(sheet);
                    StatusChange[] change = new StatusChange[1];
                    // The new claim's opener is the user, who may still settle it.
                    String openedBy = into.getOpenedBy();
                    into.setOpenedBy(null);
                    try {
                        new ApplyStatus().apply(status, move, into, store, people, rates, settings, sheets, periods,
                                voidReasons, c -> change[0] = c);
                    } finally {
                        into.setOpenedBy(openedBy);
                    }
                    new SaveClaim().save(status, into, store, log);
                    new AuditClaimStatus().audit(status, move, into, change[0], beforeSettle, people, audit, rollups,
                            watches);
                    return true;
                } catch (InvalidSheetException e) {
                    refused[0] = e;
                    tx.setRollbackOnly();
                    return false;
                }
            }));
        } finally {
            if (done) {
                audit.release();
            } else {
                audit.discard();
            }
        }
        if (refused[0] != null) {
            throw refused[0];
        }
    }
}

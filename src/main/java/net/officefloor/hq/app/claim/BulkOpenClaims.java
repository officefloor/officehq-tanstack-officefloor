package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.flag.FlagRepository;
import net.officefloor.hq.app.period.PeriodCap;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Open each selected claim that can be opened, in list order (people in list order, newest
 *  claim first within a person), leaving each unnumbered (a claim is numbered when settled). Each is opened as a single open would be,
 *  with its OPEN line in its status log, its CLAIM_OPENED line and then how the sheet's figures moved over it. A selected claim that cannot be
 *  opened (the sheet is archived, the user's role may not open, not on the sheet, not a DRAFT, dated in a closed period, its person already at its period's cap of OPEN or PENDING claims, or with an empty note and a due above the note threshold in force on its date, or its person flagged in force and the user not a steward) is left as it is and counted as refused. One whose due is above the review threshold in force on
 *  its date goes to REVIEW instead, with a CLAIM_REVIEW line, and is counted as opened. */
public class BulkOpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @Val BulkOpenInput input, ClaimRepository claims, ClaimLogRepository log,
            PersonRepository people, PeriodRepository periods, RateRepository rates, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            FlagRepository flags, Rollups rollups, Audit audit, Out<BulkOpenResponse> result) {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        int opened = 0;
        if (sheets.archived(sheet)) {
            result.set(new BulkOpenResponse(0, input.claimIds().size()));
            return;
        }
        if (!settings.permissions(sheet).allows("open", roles.of(sheet, input.user()))) {
            result.set(new BulkOpenResponse(0, input.claimIds().size()));
            return;
        }
        for (Person person : people.list(sheet)) {
            List<Claim> theirs = claims.listForPerson(sheet, person.id()).stream()
                    .filter(c -> input.claimIds().contains(c.id())).toList();
            for (Claim claim : theirs) {
                if (claim.status().canBecome(ClaimStatus.OPEN) && periods.closedOn(sheet, claim.dueBy()) == null
                        && PeriodCap.reached(claim, periods, claims) == null
                        && ClaimStatusChange.noteRequired(claim, claims, people, rates, settings, sheets) == null
                        && !flagged(claim, input.user(), roles, flags)) {
                    Rollup before = rollups.take(sheet);
                    boolean review = ClaimStatusChange.reviewRequired(claim, claims, people, rates, settings, sheets);
                    claims.changeStatus(claim, review ? ClaimStatus.REVIEW : ClaimStatus.OPEN, claim.number(),
                            claim.settledDue(), input.user());
                    log.record(claim, input.user());
                    audit.record((review ? "CLAIM_REVIEW" : "CLAIM_OPENED") + " sheet=" + sheet + " by=" + input.user()
                            + " person=" + person.name() + " amount=" + claim.amount().toPlainString()
                            + " date=" + claim.date() + (claim.number() != null ? " number=" + claim.number() : ""));
                    rollups.record(sheet, input.user(), before);
                    opened++;
                }
            }
        }
        result.set(new BulkOpenResponse(opened, input.claimIds().size() - opened));
    }

    private static boolean flagged(Claim claim, String user, RoleRepository roles, FlagRepository flags) {
        try {
            RefuseFlaggedOpen.refuse(claim, user, roles, flags);
            return false;
        } catch (ClaimStatusNotAllowedException e) {
            return true;
        }
    }
}

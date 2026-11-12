package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimAdding;
import net.officefloor.hq.app.claim.ClaimLineStore;
import net.officefloor.hq.app.claim.ClaimLogStore;
import net.officefloor.hq.app.claim.ClaimOpening;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.voidreason.VoidReasonStore;
import net.officefloor.hq.app.watch.WatchStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Takes each pasted line in order: adds its claim as the claim form would (a DRAFT recorded as
 *  CLAIM_ADDED), or edits the DRAFT it matched to its amount (CLAIM_EDITED when that changes), then
 *  opens it by the user exactly as the open button would. All or nothing: the first refusal rolls every
 *  line back, drops their audit records and refuses the paste. */
public class SavePasted {
    public void save(@Val PasteInput pasted, ClaimStore store, SettingStore settings, ClaimLogStore log,
            PeriodStore periods, RollupService rollups, Audit audit, HolidayStore holidays, PersonStore people,
            SheetStore sheets, RoleStore roles, RateStore rates, WatchStore watches, VoidReasonStore voidReasons,
            ClaimLineStore lines, PlatformTransactionManager transactions, Out<PasteResult> result)
            throws InvalidSheetException {
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        InvalidSheetException[] refused = new InvalidSheetException[1];
        audit.hold();
        boolean done = false;
        try {
            done = Boolean.TRUE.equals(nested.execute(tx -> {
                int line = 0;
                for (PastedClaim p : pasted.claims()) {
                    line++;
                    try {
                        Claim claim;
                        if (p.draftId() == null) {
                            claim = ClaimAdding.add(p.claim(), p.person(), store, settings, log, periods, rollups,
                                    audit, holidays);
                        } else {
                            claim = store.find(p.person().getSheetId(), p.draftId());
                            if (claim == null) {
                                throw new InvalidSheetException("The claim it matched is no longer on the sheet");
                            }
                            ClaimOpening.editAmount(claim, p.claim(), store, lines, people, rollups, audit);
                        }
                        ClaimOpening.open(claim, p.claim().user(), store, people, periods, settings, sheets, roles,
                                rates, log, rollups, audit, watches, voidReasons);
                    } catch (InvalidSheetException e) {
                        refused[0] = new InvalidSheetException("Line " + line + ": " + e.getMessage());
                        tx.setRollbackOnly();
                        return false;
                    }
                }
                return true;
            }));
        } finally {
            if (done) {
                audit.release();
            } else {
                audit.discard();
            }
        }
        if (!done) {
            throw refused[0] != null ? refused[0] : new InvalidSheetException("The paste was refused");
        }
        result.set(new PasteResult(pasted.claims().size()));
    }
}

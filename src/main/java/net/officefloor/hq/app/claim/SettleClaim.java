package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;
import net.officefloor.hq.app.asof.AsOfStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Settles a claim, fixing its due: its amount at its person's grade's rate in force on its date, rounded by the sheet's policy date to
 *  two decimals (a claim bound by a statement: the figure it was bound to). With no rate the due is not
 *  fixed and stays live. Its late charge on the sheet's today is added, then the sheet's floor applied last, and fixed with it. Refused for the user who opened it or last edited it (four eyes), and for a claim
 *  that is not OPEN, and for a role the permission matrix does not let settle. Settled by a clerk when the due is above the clerk limit in force (its person's grade's on the sheet's today when set, else the sheet's; a due that is n/a is within any limit; a steward settles
 *  anything), it goes to PENDING instead (after the proof threshold: one above it with no proofs is refused), nothing fixed, until a steward approves or rejects it. */
public class SettleClaim {
    /** Settled as of a date ({@code on}; blank: the sheet's today): the frozen due is the claim's due as it stood
     *  at the end of that day, as the sheet shown as of it works it out. Refused for a date after the sheet's today,
     *  and for a claim that did not exist then. */
    public void move(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "on", required = false) String on, @Val Claim claim,
            PersonRepository people, RateRepository rates, PeriodRepository periods,
            RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            ClaimRepository claims, AsOfStore store)
            throws ClaimStatusException, PeriodClosedException {
        Supplier<String> asOfDue = null;
        if (on != null && !on.isBlank()) {
            LocalDate date;
            try {
                date = LocalDate.parse(on.trim());
            } catch (DateTimeParseException e) {
                throw new ClaimStatusException("Not a date: " + on);
            }
            if (date.isAfter(sheets.today(claim.getSheetId()))) {
                throw new ClaimStatusException("A claim cannot be settled as of a date after the sheet's today");
            }
            String[] then = store.read(claim.getSheetId(), date, () -> {
                Claim was = claims.find(claim.getSheetId(), claim.getId());
                if (was == null) return null;
                return new String[] { dueOf(was, people.find(was.getSheetId(), was.getPersonId()), rates, settings, sheets) };
            });
            if (then == null) {
                throw new ClaimStatusException("The claim did not exist on " + date);
            }
            asOfDue = () -> then[0];
            claim.setSettledAsOf(date.toString());
        }
        settle(by, claim, true, people, rates, periods, roles, settings, sheets, asOfDue);
        ClaimNumbers.settled(claim, claims, periods, settings, sheets);
        // Settled one at a time: in no batch.
        claim.setSettleBatch(null);
    }

    /** The rules of settling one claim, shared with {@link BulkSettleClaims}: moves the claim (unsaved) or throws. */
    static void settle(String by, Claim claim, PersonRepository people, RateRepository rates,
            PeriodRepository periods, RoleRepository roles, SettingRepository settings, SheetRepository sheets)
            throws ClaimStatusException, PeriodClosedException {
        settle(by, claim, true, people, rates, periods, roles, settings, sheets);
    }

    /** As {@link #settle}, the four-eyes and proof rules judged on the claim itself only when {@code judged}
     *  (a part-settle judges them on the original claim, by {@link #judge}, before the split). */
    static void settle(String by, Claim claim, boolean judged, PersonRepository people, RateRepository rates,
            PeriodRepository periods, RoleRepository roles, SettingRepository settings, SheetRepository sheets)
            throws ClaimStatusException, PeriodClosedException {
        settle(by, claim, judged, people, rates, periods, roles, settings, sheets, null);
    }

    /** As {@link #settle}, the due frozen being {@code asOfDue}'s when given (else the due now). */
    static void settle(String by, Claim claim, boolean judged, PersonRepository people, RateRepository rates,
            PeriodRepository periods, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            Supplier<String> asOfDue) throws ClaimStatusException, PeriodClosedException {
        if (!Permissions.allows(claim.getSheetId(), by, Permissions.SETTLE, roles, settings)) {
            throw new ClaimStatusException("Your role may not settle a claim");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        if (!ClaimStatus.OPEN.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be settled");
        }
        if (claim.isDeferred()) {
            throw new ClaimStatusException("A deferred claim cannot be settled until " + claim.getDeferredUntil());
        }
        ClaimStatus settled = ClaimStatus.OPEN.moveTo(ClaimStatus.SETTLED);
        if (judged) requireFourEyes(by, claim);
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        String due = asOfDue != null ? asOfDue.get() : dueOf(claim, person, rates, settings, sheets);
        if (judged) requireProof(claim, person, due, settings);
        if (due != null && Role.CLERK.equals(roles.roleOf(claim.getSheetId(), by == null ? "" : by))) {
            BigDecimal limit = settings.clerkLimit(claim.getSheetId(),
                    person == null ? null : person.gradeOn(sheets.today(claim.getSheetId()).toString()));
            if (limit != null && new BigDecimal(due).compareTo(limit) > 0) {
                claim.setStatus(ClaimStatus.OPEN.moveTo(ClaimStatus.PENDING).name());
                return;
            }
        }
        claim.setStatus(settled.name());
        claim.setSettledDue(due);
    }

    /** The four-eyes and proof rules of settling, judged on the claim (unchanged) alone. */
    static void judge(String by, Claim claim, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) throws ClaimStatusException {
        requireFourEyes(by, claim);
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        requireProof(claim, person, dueOf(claim, person, rates, settings, sheets), settings);
    }

    /** Refuses the user who opened the claim or last edited it. */
    static void requireFourEyes(String by, Claim claim) throws ClaimStatusException {
        if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(by == null ? "" : by)) {
            throw new ClaimStatusException("The user who opened a claim cannot settle it");
        }
        if (claim.getEditedBy() != null && claim.getEditedBy().equals(by == null ? "" : by)) {
            throw new ClaimStatusException("The user who last edited a claim cannot settle it");
        }
    }

    /** The due the claim settles at now (null: n/a). */
    static String dueOf(Claim claim, Person person, RateRepository rates, SettingRepository settings,
            SheetRepository sheets) {
        Rate rate = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()),
                LocalDate.parse(claim.getDate()), claim.getId());
        // A claim bound by a statement settles at the figure it was bound to.
        // Its late charge on the sheet's today is frozen in.
        Dues.Late late = settings.late(claim.getSheetId(), sheets.today(claim.getSheetId()));
        return Dues.shown(claim, rate == null ? null : Dues.of(claim.getAmount(), rate.pct(),
                late.rounding(claim.getRoundDate())), settings.floor(claim.getSheetId()).on(claim.getDate()), late);
    }

    /** Refuses settling or approving a claim with no proofs (while the sheet's proof kinds are set: with
     *  none of one of them) whose due (null: n/a, never above) is above
     *  the proof threshold in force on the claim's date: its person's grade's (the grade in force on that
     *  date) when one is set, else the sheet's. */
    static void requireProof(Claim claim, Person person, String due, SettingRepository settings) throws ClaimStatusException {
        if (due == null) return;
        java.util.List<String> kinds = settings.proofKinds(claim.getSheetId());
        if (claim.getProofs() > 0 && kinds.stream().allMatch((kind) -> claim.proofsOf(kind) > 0)) return;
        BigDecimal threshold = settings.proofThreshold(claim.getSheetId(),
                person == null ? null : person.gradeOn(claim.getDate()), claim.getDate());
        if (threshold != null && new BigDecimal(due).compareTo(threshold) > 0) {
            throw new ClaimStatusException("A claim whose due is above the proof threshold needs a proof"
                    + (kinds.isEmpty() ? "" : " of each kind: " + String.join(",", kinds)));
        }
    }
}

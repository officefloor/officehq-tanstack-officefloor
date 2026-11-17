package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to its new status: DRAFT to OPEN, OPEN to SETTLED (or PENDING), PENDING to SETTLED (approve) or back
 * to OPEN (reject), DRAFT, OPEN or PENDING to VOID. A voided claim keeps
 * its number (never given again). Settling fixes the claim's due: its amount at its
 * person's grade's rate in force on the claim's date, rounded by its rounding (see Rounding) and at least the sheet's floor (left live when no rate applies),
 * or the figure a statement bound it to, with its late charge as of the sheet's today frozen in (see LateCharge).
 * Opening records who opened the claim; that user cannot settle nor approve it (four eyes), nor can the user who last
 * edited it (see ClaimRepository#editedBy). A claim whose due is
 * above the proof threshold in force on its date (its person's grade's on that date when one is set, else the sheet's; if any) cannot be settled nor approved while its proofs
 * count is 0, nor while the sheet's proof kinds are set and it has no proof of one of them (checked before the clerk limit, so it never goes PENDING); a due of n/a is within any threshold. A claim whose due-by falls inside a closed
 * period cannot change status, nor can a deferred claim be settled (see ClaimDue#deferred). A clerk (any user not made a steward) settling a claim whose due is above the sheet's
 * clerk limit, if it has one, makes it PENDING instead (its due left unfixed, still bound by its statement if one binds it); a due of n/a is
 * within any limit. A steward settles anything. Only a steward approves a PENDING claim (fixing its figure, live or bound by a statement, late
 * charge included, at that moment) or rejects it (OPEN again, still bound by its statement if one binds it).
 * A claim whose due (as it shows: its live figure at least the floor, or the figure a statement bound it to) is above
 * the sheet's note threshold in force on its date (if it has one) cannot be opened while its note is empty; a due of
 * n/a is within any threshold (see #needsNote). Nor can a claim be opened when its person already has as many OPEN or PENDING
 * claims in the period its due-by falls in as the period's cap (see ClaimRules#overCap).
 * Opening, settling and voiding are each taken only by the roles the sheet's permission allows (see SettingRepository#permits).
 * While the sheet has void reasons, a void needs one of them (see VoidReasonRepository); a reason not on its list is refused.
 * Only a steward voids a SETTLED claim, and only within 7 days of its settlement (the sheet's today on its status log's
 * latest SETTLED line) by the sheet's today; its frozen figure is kept on it but, VOID, it leaves every total.
 * Only a steward re-settles a SETTLED claim (not one whose due-by falls inside a closed period): its due is fixed again at the figure
 * it would have today as an OPEN claim bound by no statement (today's rates, grades, floor and late charge).
 * A DRAFT claim (not one whose due-by falls inside a closed period) is deleted (DELETED: it counts nowhere), and a DELETED
 * one restored (DRAFT again), but only on the sheet's today it was deleted (its status log's latest DELETED line).
 * Opening a claim whose due is above the sheet's review threshold in force on its date (if it has one) makes it REVIEW
 * instead, unnumbered, recording who opened it (see #needsReview); only a steward reviews it, making it OPEN (numbered
 * then). A REVIEW claim may otherwise only be voided.
 * A settle made as of a date (on or before the sheet's today; a later one is refused, as is one before the claim was
 * added) fixes the claim's due as it stood at the end of that date (see ClaimDueAsOf) instead of today's.
 * While the claim's person is flagged (the sheet's today on or before the flag's until-date) only a steward opens it.
 */
public class ApplyClaimStatus {

    /** What proof the claim lacks to pass its proof threshold: "" when it has none at all, the first of the sheet's
     *  proof kinds it has no proof of, or null when it lacks none. */
    static String missingProof(Claim claim, SettingRepository settings, ClaimRepository claims) {
        if (claim.proofs() == 0) {
            return "";
        }
        int photos = claims.photoProofs(claim);
        for (String kind : settings.proofKinds(claim.sheetId())) {
            int count = SettingRepository.DEFAULT_KIND.equals(kind) ? claim.proofs() - photos : photos;
            if (count == 0) {
                return kind;
            }
        }
        return null;
    }

    /** How many days after its settlement a steward may still void a SETTLED claim. */
    static final int VOID_SETTLED_DAYS = 7;

    public void apply(@Val Claim claim, @Val ClaimStatusChange change, PersonRepository people, RateRepository rates,
            PeriodRepository periods, SettingRepository settings, RoleRepository roles, SheetRepository sheets, ClaimRepository claims, ClaimLog log, VoidReasonRepository voidReasons, ClaimLogRepository logs, StatementRepository statements, Out<Claim> changed, Out<ClaimNumber> number, Out<PreviousDue> previous) throws InvalidClaimStatusException {
        BigDecimal[] asOfDue = null;
        if (change.action() == ClaimAction.SETTLE && change.on() != null) {
            if (change.on().isAfter(sheets.today(claim.sheetId()))) {
                throw new InvalidClaimStatusException("A claim cannot be settled as of a date after the sheet's today");
            }
            asOfDue = new BigDecimal[] { ClaimDueAsOf.of(claim, change.on(), claims, logs, people, rates, settings,
                    statements).value() };
        }
        applyJudged(claim, claim, change, people, rates, periods, settings, roles, sheets, claims, log, voidReasons,
                changed, number, previous, asOfDue);
    }

    /** As #apply, but the four-eyes rules and the proof rule are judged on the claim given as judged (a part-settle
     *  judges them on the claim it splits from, see SettlePart) instead of on the claim changed. */
    static void applyJudged(Claim claim, Claim judged, ClaimStatusChange change, PersonRepository people, RateRepository rates,
            PeriodRepository periods, SettingRepository settings, RoleRepository roles, SheetRepository sheets, ClaimRepository claims, ClaimLog log, VoidReasonRepository voidReasons, Out<Claim> changed, Out<ClaimNumber> number, Out<PreviousDue> previous) throws InvalidClaimStatusException {
        applyJudged(claim, judged, change, people, rates, periods, settings, roles, sheets, claims, log, voidReasons,
                changed, number, previous, null);
    }

    /** As above; a settle given its due as of a date (asOfDue: a one-element array, its element null for n/a; null
     *  for none, see ClaimDueAsOf) fixes that due instead of today's. */
    static void applyJudged(Claim claim, Claim judged, ClaimStatusChange change, PersonRepository people, RateRepository rates,
            PeriodRepository periods, SettingRepository settings, RoleRepository roles, SheetRepository sheets, ClaimRepository claims, ClaimLog log, VoidReasonRepository voidReasons, Out<Claim> changed, Out<ClaimNumber> number, Out<PreviousDue> previous, BigDecimal[] asOfDue) throws InvalidClaimStatusException {
        boolean allowed = switch (change.action()) {
            case OPEN -> claim.status() == ClaimStatus.DRAFT;
            case SETTLE -> claim.status() == ClaimStatus.OPEN;
            case APPROVE, REJECT -> claim.status() == ClaimStatus.PENDING;
            case VOID -> claim.status() == ClaimStatus.DRAFT || claim.status() == ClaimStatus.OPEN
                    || claim.status() == ClaimStatus.PENDING || claim.status() == ClaimStatus.SETTLED
                    || claim.status() == ClaimStatus.REVIEW;
            case RESETTLE -> claim.status() == ClaimStatus.SETTLED;
            case DELETE -> claim.status() == ClaimStatus.DRAFT;
            case RESTORE -> claim.status() == ClaimStatus.DELETED;
            case REVIEW -> claim.status() == ClaimStatus.REVIEW;
        };
        if (!allowed) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim cannot be made " + change.to());
        }
        if (periods.closedOn(claim.sheetId(), claim.dueBy())) {
            throw new InvalidClaimStatusException("The claim's due-by is inside a closed period");
        }
        if (change.action() == ClaimAction.RESTORE && !sheets.today(claim.sheetId()).equals(log.deletedOn(claim))) {
            throw new InvalidClaimStatusException("A deleted claim can only be restored on the sheet-today it was deleted");
        }
        if (change.action() == ClaimAction.SETTLE && ClaimDue.deferred(claim, sheets.today(claim.sheetId()))) {
            throw new InvalidClaimStatusException("A deferred claim cannot be settled until the date it is deferred until");
        }
        String role = roles.roleOf(claim.sheetId(), change.by());
        boolean steward = RoleRepository.STEWARD.equals(role);
        String permission = switch (change.action()) {
            case OPEN -> "open";
            case SETTLE -> "settle";
            case VOID -> "void";
            default -> null;
        };
        if (permission != null && !settings.permits(claim.sheetId(), permission, role)) {
            throw new InvalidClaimStatusException("A " + role + " may not " + permission + " a claim");
        }
        if (change.action() == ClaimAction.OPEN && !steward && flagged(claim, people, sheets)) {
            throw new InvalidClaimStatusException("Only a steward may open a claim of a flagged person");
        }
        if (change.action() == ClaimAction.VOID && !change.reason().isEmpty()
                ? !voidReasons.has(claim.sheetId(), change.reason())
                : change.action() == ClaimAction.VOID && voidReasons.any(claim.sheetId())) {
            throw new InvalidClaimStatusException("A void needs a reason from the sheet's list of void reasons");
        }
        if (change.action() == ClaimAction.OPEN && needsNote(claim, people, rates, settings,
                LateCharge.of(claim.sheetId(), sheets, settings, claims))) {
            throw new InvalidClaimStatusException("A claim whose due is above the note threshold needs a note to be opened");
        }
        if (change.action() == ClaimAction.OPEN && ClaimRules.overCap(claim, periods, claims)) {
            throw new InvalidClaimStatusException("The person already has as many open claims in the period as its cap allows");
        }
        if ((change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE)
                && change.by().equals(judged.openedBy())) {
            throw new InvalidClaimStatusException("The user who opened a claim cannot settle nor approve it");
        }
        if ((change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE)
                && change.by().equals(claims.editedBy(judged))) {
            throw new InvalidClaimStatusException("The user who last edited a claim cannot settle nor approve it");
        }
        if ((change.action() == ClaimAction.APPROVE || change.action() == ClaimAction.REJECT) && !steward) {
            throw new InvalidClaimStatusException("Only a steward approves or rejects a pending claim");
        }
        if (change.action() == ClaimAction.VOID && claim.status() == ClaimStatus.SETTLED) {
            if (!steward) {
                throw new InvalidClaimStatusException("Only a steward voids a settled claim");
            }
            LocalDate settledOn = log.settledOn(claim);
            if (settledOn == null || sheets.today(claim.sheetId()).isAfter(settledOn.plusDays(VOID_SETTLED_DAYS))) {
                throw new InvalidClaimStatusException("A settled claim can only be voided within "
                        + VOID_SETTLED_DAYS + " days of its settlement");
            }
        }
        if (change.action() == ClaimAction.REVIEW && !steward) {
            throw new InvalidClaimStatusException("Only a steward reviews a claim");
        }
        if (change.action() == ClaimAction.RESETTLE && !steward) {
            throw new InvalidClaimStatusException("Only a steward re-settles a claim");
        }
        ClaimStatus to = change.to();
        LateCharge late = LateCharge.of(claim.sheetId(), sheets, settings, claims);
        BigDecimal settledDue;
        if (change.action() == ClaimAction.RESETTLE) {
            Claim asOpen = new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                    claim.note(), ClaimStatus.OPEN, claim.number(), null, claim.openedBy(), false, null,
                    claim.corrects(), claim.dueBy(), null, claim.proofs(), claim.splitFrom(), claim.ref(), claim.tags(), claim.held());
            settledDue = late.add(asOpen, due(asOpen, people, rates, late.rounding()), settings.floor(claim.sheetId(), claim.date()));
        } else if (asOfDue != null) {
            settledDue = asOfDue[0];
        } else {
            settledDue = to == ClaimStatus.SETTLED ? late.add(claim, due(claim, people, rates, late.rounding()), settings.floor(claim.sheetId(), claim.date())) : claim.due();
        }
        BigDecimal judgedDue = judged == claim ? settledDue
                : late.add(judged, due(judged, people, rates, late.rounding()), settings.floor(judged.sheetId(), judged.date()));
        if ((change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE) && judgedDue != null
                && missingProof(judged, settings, claims) != null) {
            Person person = people.find(judged.sheetId(), judged.personId());
            BigDecimal threshold = settings.proofThreshold(judged.sheetId(),
                    person == null ? null : person.gradeOn(judged.date()), judged.date());
            if (threshold != null && judgedDue.compareTo(threshold) > 0) {
                String missing = missingProof(judged, settings, claims);
                throw new InvalidClaimStatusException(missing.isEmpty()
                        ? "A claim whose due is above the proof threshold needs a proof"
                        : "A claim whose due is above the proof threshold needs a " + missing + " proof");
            }
        }
        if (change.action() == ClaimAction.SETTLE && settledDue != null && !steward) {
            Person person = people.find(claim.sheetId(), claim.personId());
            BigDecimal limit = settings.clerkLimit(claim.sheetId(),
                    person == null ? null : person.gradeOn(sheets.today(claim.sheetId())));
            if (limit != null && settledDue.compareTo(limit) > 0) {
                to = ClaimStatus.PENDING;
                settledDue = null;
            }
        }
        if (change.action() == ClaimAction.OPEN && needsReview(claim, people, rates, settings, late)) {
            to = ClaimStatus.REVIEW;
        }
        number.set(new ClaimNumber(claim.number()));
        previous.set(new PreviousDue(claim.due(), claim.status()));
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), to, claim.number(), settledDue,
                change.action() == ClaimAction.OPEN ? change.by() : claim.openedBy(), claim.stated(), claim.statedDue(), claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom(), claim.ref(), claim.tags(), claim.held()));
    }

    /** Whether the claim's person is flagged on the sheet's today (see Person#flaggedOn): only a steward may open it. */
    static boolean flagged(Claim claim, PersonRepository people, SheetRepository sheets) {
        Person person = people.find(claim.sheetId(), claim.personId());
        return person != null && person.flaggedOn(sheets.today(claim.sheetId()));
    }

    /** Whether the claim cannot be opened for want of a note: its note is empty and its due is above the sheet's note
     *  threshold in force on its date (none: never; a due of n/a: never). */
    static boolean needsNote(Claim claim, PersonRepository people, RateRepository rates, SettingRepository settings,
            LateCharge late) {
        if (claim.note() != null && !claim.note().isBlank()) {
            return false;
        }
        BigDecimal threshold = settings.noteThreshold(claim.sheetId(), claim.date());
        if (threshold == null) {
            return false;
        }
        BigDecimal due = late.add(claim, due(claim, people, rates, late.rounding()),
                settings.floor(claim.sheetId(), claim.date()));
        return due != null && due.compareTo(threshold) > 0;
    }

    /** Whether opening the claim takes it to REVIEW: its due (as needsNote works it out) is above the sheet's review
     *  threshold in force on its date (none: never; a due of n/a: never). */
    static boolean needsReview(Claim claim, PersonRepository people, RateRepository rates, SettingRepository settings,
            LateCharge late) {
        BigDecimal threshold = settings.reviewThreshold(claim.sheetId(), claim.date());
        if (threshold == null) {
            return false;
        }
        BigDecimal due = late.add(claim, due(claim, people, rates, late.rounding()),
                settings.floor(claim.sheetId(), claim.date()));
        return due != null && due.compareTo(threshold) > 0;
    }

    private static BigDecimal due(Claim claim, PersonRepository people, RateRepository rates, Rounding rounding) {
        if (ClaimDue.bound(claim)) {
            return claim.statedDue();
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        return person == null ? null : ClaimDue.live(claim, person.gradeOn(claim.date()), rates, rounding);
    }
}

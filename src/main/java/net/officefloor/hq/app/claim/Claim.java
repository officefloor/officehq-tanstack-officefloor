package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.setting.Dated;

/**
 * A claim as the screen shows it: amount with two decimals, date (ISO yyyy-MM-dd), note, status and its number on
 * the sheet (given when opened and kept once voided; null while a draft), and its due fixed when settled (two decimals;
 * null until settled, and when the grade had no rate then), and the user who opened it (null until opened), and
 * whether its person's last statement bound it, with the figure it bound (null when the grade had no rate then), and
 * the number of the claim it corrects (null unless it is a correction), and the date it rounds on: its own, or for
 * a correction the date of the claim it corrects, and its number within its period (<period>-<n>, or X-<n> in no
 * period) when it was opened while the sheet numbered by period, in place of a number on the sheet (null otherwise),
 * and its due-by (yyyy-MM-dd), fixed when it was added from the terms in force on the sheet's today then (its person's own, their grade's, the sheet's),
 * and its status log, oldest first, and the date it was last deferred until (null when never), with whether it is
 * deferred now: OPEN (or VOID, voided while deferred) with that date after the sheet's today, and its proofs count, and the number (as it shows) of
 * the claim it was split off (null unless it was), and the user whose open editor locks it (null when none does),
 * and whether it was added while its period was soft-closed (a late entry), and the sheet's today it was last
 * reminded on (null when never), and its external reference (null for none), and its tags (lower-cased, sorted),
 * with every tag it has ever carried, and its comments, in the order written, and the number of the bulk settle
 * batch that last settled it (null when settled one at a time, or never), and its lines, in the order added, and
 * when it was added (the claim sequence's value; null when kept from before it was recorded), which decides the
 * forward-only rates it sees, and whether a steward waived its late charge (it then bears none), and how many of its
 * proofs are photos (the rest are receipts), and whether it is held (toggled while OPEN or PENDING): while held and
 * not bound by a statement it counts in no due figure (it stays in claimed).
 */
public record Claim(long id, long person, String amount, String date, String note, ClaimStatus status,
        Integer number, String settledDue, String openedBy, boolean stated, String statedDue, String corrects,
        String roundsOn, String periodNumber, String dueBy, List<ClaimLogLine> log, String deferredUntil, boolean deferred, int proofs,
        String splitFrom, String lockedBy, boolean lateEntry, String remindedOn, String ref, List<String> tags,
        List<String> tagsEver, List<ClaimComment> comments, Integer batch, List<ClaimLine> lines, Long added, boolean waived, int photos, boolean held) {

    /** Whether this claim is a correction: a negative amount, naming the number (as it shows) of the settled claim it corrects. */
    public boolean correction() {
        return amount.startsWith("-");
    }

    /** How many of its proofs are of the kind (receipt or photo). */
    public int proofsOf(String kind) {
        return ProofKind.PHOTO.equals(kind) ? photos : ProofKind.RECEIPT.equals(kind) ? proofs - photos : 0;
    }

    /** The audit fields of this claim for the named person, as {@link ClaimInput#auditFields}. */
    public String auditFields(String person) {
        return "person=" + person + " amount=" + amount + " date=" + date;
    }

    /** This claim, numbered: its audit fields end with number=<number> when it has one. */
    public Claim withNumber(Integer number) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferred, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** This claim, numbered within its period: its audit fields end with number=<period number>. */
    public Claim withPeriodNumber(String periodNumber) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferred, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** This claim with another date (yyyy-MM-dd), as once re-dated. */
    public Claim withDate(String date) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferred, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** This claim with another amount (two decimals), as its remaining amount once split. */
    public Claim withAmount(String amount) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferred, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** This claim, settled with its due fixed (null when its grade had no rate). */
    public Claim withSettledDue(String settledDue) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferred, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** This claim as an OPEN claim bound by no statement and with no due fixed: the figure it would have live. */
    public Claim asOpenUnbound() {
        return new Claim(id, person, amount, date, note, ClaimStatus.OPEN, number, null, openedBy, false, null, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferred, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** This claim as it stands with the date for the sheet's today: deferred while OPEN (or VOID) with its deferred-until after it. */
    public Claim on(LocalDate today) {
        boolean deferredThen = (status == ClaimStatus.OPEN || status == ClaimStatus.VOID) && deferredUntil != null && LocalDate.parse(deferredUntil).isAfter(today);
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferredThen, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** This claim, with its comments (in the order written). */
    public Claim withComments(List<ClaimComment> comments) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferred, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** This claim, with its lines (in the order added). */
    public Claim withLines(List<ClaimLine> lines) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferred, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** This claim, with its status log (oldest first). */
    public Claim withLog(List<ClaimLogLine> log) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber, dueBy, log, deferredUntil, deferred, proofs, splitFrom, lockedBy, lateEntry, remindedOn, ref, tags, tagsEver, comments, batch, lines, added, waived, photos, held);
    }

    /** The date it was last settled: its status log's last SETTLED line (null when the log has none). */
    public LocalDate settledOn() {
        LocalDate on = null;
        for (ClaimLogLine line : log == null ? List.<ClaimLogLine>of() : log) {
            if (line.status() == ClaimStatus.SETTLED) {
                on = LocalDate.parse(line.on());
            }
        }
        return on;
    }

    /** The date it was last deleted: its status log's last DELETED line (null when the log has none). */
    public LocalDate deletedOn() {
        LocalDate on = null;
        for (ClaimLogLine line : log == null ? List.<ClaimLogLine>of() : log) {
            if (line.status() == ClaimStatus.DELETED) {
                on = LocalDate.parse(line.on());
            }
        }
        return on;
    }

    /** The number the claim shows: its number within its period, else its number on the sheet; null for none. */
    public String shownNumber() {
        return periodNumber != null ? periodNumber : number == null ? null : number.toString();
    }

    /** The audit fields of a status change: {@link #auditFields} then number=<number> when it has one. */
    public String statusAuditFields(String person) {
        String shown = shownNumber();
        return auditFields(person) + (shown == null ? "" : " number=" + shown);
    }

    /** Whether this claim counts toward the totals: OPEN, PENDING and SETTLED claims do, unless deferred. */
    public boolean counts() {
        return !deferred && (status == ClaimStatus.OPEN || status == ClaimStatus.PENDING || status == ClaimStatus.SETTLED);
    }

    /**
     * The status this claim had at the end of the day, as its status log records it (the last line on or before it);
     * null when it was not yet added then. A claim kept from before the log (no lines) has its status now.
     */
    public ClaimStatus statusOn(LocalDate end) {
        if (log == null || log.isEmpty()) {
            return status;
        }
        ClaimStatus then = null;
        for (ClaimLogLine line : log) {
            if (!LocalDate.parse(line.on()).isAfter(end)) {
                then = line.status();
            }
        }
        return then;
    }

    /** Whether this claim's statement binds its figure: it is stated (a PENDING claim too, like any that counts). */
    public boolean bound() {
        return stated;
    }

    /**
     * Whether this claim keeps a figure whatever the rates: fixed when settled, or bound by a statement (a claim
     * settled or bound when its grade had no rate keeps none).
     */
    public boolean fixed() {
        return settledDue != null || (bound() && statedDue != null);
    }

    /**
     * This claim's due: the due fixed when settled; else the due its statement bound (as shown then, late charge
     * included); else its floored figure (the greater of the sheet's floor in force on its date and its {@link #figure})
     * plus, while it is overdue, its late charge on that floored figure (a correction has no floor and no charge); null
     * when there is no figure.
     */
    public BigDecimal due(List<Rate> rates, String grade, Dated floor, Late late) {
        BigDecimal figure = figure(rates, grade, floor, late.rounding(this));
        if (figure == null || settledDue != null || bound() || correction()) {
            return figure;
        }
        BigDecimal floored = figure.max(floor.on(date));
        return floored.add(late.charge(this, grade, floored));
    }

    /**
     * This claim's figure: the due fixed when settled, else the due its statement bound (as shown, late charge
     * included), else
     * its amount at the grade's rate in force on its date, of the rates it sees (the floor is applied before the late charge, in {@link #due}); null when
     * there is none.
     */
    public BigDecimal figure(List<Rate> rates, String grade, Dated floor) {
        return figure(rates, grade, floor, RoundingMode.HALF_EVEN);
    }

    /** This claim's {@link #figure}, its live figure rounded by the mode. */
    public BigDecimal figure(List<Rate> rates, String grade, Dated floor, RoundingMode rounding) {
        if (settledDue != null) {
            return new BigDecimal(settledDue);
        }
        if (bound()) {
            return statedDue == null ? null : new BigDecimal(statedDue);
        }
        return Rate.due(amount, Rate.on(Rate.seenBy(rates, added), grade, LocalDate.parse(date)), rounding);
    }
}

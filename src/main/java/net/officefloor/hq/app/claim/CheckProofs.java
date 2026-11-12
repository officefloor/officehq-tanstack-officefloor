package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;

/** Refuses settling an OPEN claim, or approving a PENDING one, while it has no proofs (or, while the sheet lists proof
 *  kinds, lacks a proof of any kind listed) and its due is above
 *  the proof threshold in force on its date (its person's grade's then when one is set, else the sheet's). A sheet with no threshold then, and a claim whose due is n/a,
 *  need no proof. */
final class CheckProofs {
    private CheckProofs() {
    }

    static void refuseUnproved(StatusInput input, Claim claim, SettingStore settings, PersonStore people,
            RateStore rates, SheetStore sheets, ClaimStore claims) throws InvalidSheetException {
        ClaimStatus from = claim.getStatus();
        if (input.status() != ClaimStatus.SETTLED || (from != ClaimStatus.OPEN && from != ClaimStatus.PENDING)
                || proved(claim, settings.proofKinds(claim.getSheetId()))) {
            return;
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        String grade = person == null ? null : person.gradeOn(claim.getDate());
        BigDecimal threshold = settings.proofThreshold(claim.getSheetId(), grade, claim.getDate());
        if (threshold == null) {
            return;
        }
        BigDecimal pct = person == null ? null : rates.forClaim(claim).find(claim.getSheetId(), grade, claim.getDate());
        BigDecimal due = Lateness.of(claim.getSheetId(), settings, sheets, claims, people).due(claim, pct);
        if (due != null && due.compareTo(threshold) > 0) {
            throw new InvalidSheetException("A claim due above the proof threshold of " + threshold.setScale(2)
                    + " needs " + (claim.getProofs() == 0 ? "a proof" : "a proof of each kind listed")
                    + " before it can be " + (input.review() ? "approved" : "settled"));
        }
    }

    /** Whether the claim has a proof, and at least one of each kind listed. */
    private static boolean proved(Claim claim, List<String> kinds) {
        return claim.getProofs() > 0 && kinds.stream().allMatch(kind -> claim.proofsOf(kind) > 0);
    }

    /** Refuses opening a DRAFT claim with an empty note while its due is above the note threshold in force on
     *  its date. A sheet with no threshold then, and a claim whose due is n/a, need no note. */
    static void refuseUnnoted(Claim claim, SettingStore settings, PersonStore people, RateStore rates,
            SheetStore sheets, ClaimStore claims) throws InvalidSheetException {
        if (claim.getNote() != null && !claim.getNote().isBlank()) {
            return;
        }
        BigDecimal threshold = settings.noteThreshold(claim.getSheetId(), claim.getDate());
        if (threshold == null) {
            return;
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal pct = person == null ? null : rates.forClaim(claim).find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
        BigDecimal due = Lateness.of(claim.getSheetId(), settings, sheets, claims, people).due(claim, pct);
        if (due != null && due.compareTo(threshold) > 0) {
            throw new InvalidSheetException("A claim due above the note threshold of " + threshold.setScale(2)
                    + " needs a note before it can be opened");
        }
    }
}

package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Settles the claim and fixes its due: its amount times the rate of its person's grade in force on its date, in force on the
 * claim's date, divided by 100 and rounded to two decimals as the claim rounds (by the sheet's policy date) and held to
 * at least the floor in force on its date, or for a claim a statement bound its bound figure; with the late
 * charge it has gained on that floored figure by the sheet's today frozen in on top.
 * With no such rate the due is not fixed. The user who opened the claim, or last edited it, may not settle it.
 * When a clerk (any user not made a steward) settles a claim whose due is above the sheet's clerk
 * limit, it goes to PENDING instead, its due left live until a steward approves or rejects it; a
 * due of n/a is within any limit. Before the clerk limit is considered, a claim whose due is above
 * the proof threshold in force on its date is refused while it has no proofs, or none of a kind the proof kinds list. Only a role the
 * permission matrix allows may settle.
 * <p>
 * Settled as of a date (on or before the sheet's today), the due fixed is the claim's due as it stood
 * at the end of that date ({@link #dueAsOf}); a date after today, or one before the claim was added,
 * is refused.
 */
public class SettleClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, @Val Settlement settlement, PersonRepository people,
            RateRepository rates, RoleRepository roles, SettingsRepository settingsStore, ClaimRepository claims,
            HttpServletRequest request) throws InvalidClaimStatusException, SelfSettleException {
        LocalDate on = settlement.on();
        if (on == null) {
            settle(sheet, claim, Audit.user(request), people, rates, roles, settingsStore);
        } else {
            if (on.isAfter(sheet.today())) {
                throw new InvalidClaimStatusException("A claim is settled as of a date on or before today");
            }
            BigDecimal[] due = { dueAsOf(sheet, claim, on, people, rates, settingsStore, claims) };
            settle(sheet, claim, Audit.user(request), people, rates, roles, settingsStore, false, due);
            claim.setSettledOn(on);
        }
        claim.setSettleBatch(null); // settled one at a time: in no batch
    }

    /**
     * The claim's due as it stood at the end of {@code on}, as the as-of view works it out: the claim,
     * its person's grades, the rates and the settings as they stood then, with {@code on} as the sheet's
     * today. Refused when the claim was not yet added then.
     */
    static BigDecimal dueAsOf(Sheet sheet, Claim claim, LocalDate on, PersonRepository people, RateRepository rates,
            SettingsRepository settingsStore, ClaimRepository claims) throws InvalidClaimStatusException {
        Claim then = claims.findAsOf(sheet.id(), claim.getId(), on);
        if (then == null) throw new InvalidClaimStatusException("The claim was not yet added on " + on);
        Person person = people.find(sheet.id(), then.getPersonId());
        if (person == null || !person.addedBy(on)) {
            throw new InvalidClaimStatusException("The claim was not yet added on " + on);
        }
        String grade = people.asOf(person, on).gradeOn(then.getDate());
        List<Rate> all = rates.findAsOf(sheet.id(), on);
        Settings settings = settingsStore.findAsOf(sheet.id(), on);
        BigDecimal figure = ClaimFigure.of(then, all, grade, settings, on);
        return figure == null ? null : ClaimFigure.due(figure, ClaimFigure.late(then, figure, grade, settings, on));
    }

    /** Settles the claim as the given user would, by the rules above; throws when it may not. */
    static void settle(Sheet sheet, Claim claim, String user, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settingsStore)
            throws InvalidClaimStatusException, SelfSettleException {
        settle(sheet, claim, user, people, rates, roles, settingsStore, false);
    }

    /**
     * Settles the claim as the given user would; when {@code judged} the four-eyes rules and the
     * proof rule were already judged on another claim (a part-settle's original) and are skipped.
     */
    static void settle(Sheet sheet, Claim claim, String user, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settingsStore, boolean judged)
            throws InvalidClaimStatusException, SelfSettleException {
        settle(sheet, claim, user, people, rates, roles, settingsStore, judged, null);
    }

    /** As above; with {@code fixed} (a one-element holder) the due is the one it holds instead of today's. */
    private static void settle(Sheet sheet, Claim claim, String user, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settingsStore, boolean judged, BigDecimal[] fixed)
            throws InvalidClaimStatusException, SelfSettleException {
        Person person = people.find(sheet.id(), claim.getPersonId());
        Settings settings = settingsStore.find(sheet.id());
        String role = roles.roleOf(sheet.id(), user);
        if (!settings.allows("settle", role)) {
            throw new InvalidClaimStatusException("A " + role + " may not settle a claim");
        }
        String grade = person.gradeOn(claim.getDate());
        BigDecimal figure = ClaimFigure.of(claim, rates.findAll(sheet.id()), grade, settings, sheet.today());
        // The late charge is read while the claim is still OPEN.
        BigDecimal late = ClaimFigure.late(claim, figure, grade, settings, sheet.today());
        if (claim.isDeferredOn(sheet.today())) {
            throw new InvalidClaimStatusException("A claim deferred until " + claim.getDeferredUntil() + " cannot be settled");
        }
        if (!claim.getStatus().canMoveTo(ClaimStatus.SETTLED)) {
            throw new InvalidClaimStatusException(claim.getStatus(), ClaimStatus.SETTLED);
        }
        BigDecimal due = fixed != null ? fixed[0] : ClaimFigure.due(figure, late);
        if (!judged) judge(claim, user, due, grade, settings);
        if (!settings.withinClerkLimit(due, grade, claim.getDate()) && (Role.CLERK.equals(role) || Role.RESTRICTED.equals(role))) {
            claim.moveTo(ClaimStatus.PENDING);
            return;
        }
        claim.moveTo(ClaimStatus.SETTLED);
        claim.setSettledDue(due);
    }

    /**
     * The four-eyes rules and the proof rule: refuses the user who opened the claim or last edited it,
     * and a claim whose due is above the proof threshold without the proofs it needs.
     */
    static void judge(Claim claim, String user, BigDecimal due, String grade, Settings settings)
            throws InvalidClaimStatusException, SelfSettleException {
        if (user.equals(claim.getOpenedBy())) throw new SelfSettleException(user);
        if (claim.isOwnedBy(user)) throw new SelfSettleException(user, "last edited");
        if (settings.needsProof(due, grade, claim.getDate(), claim)) throw new InvalidClaimStatusException(ProofRequired.MESSAGE);
    }

    /** The due the claim would be settled at now, by the rules above; null for n/a. */
    static BigDecimal dueNow(Sheet sheet, Claim claim, String grade, RateRepository rates, Settings settings) {
        BigDecimal figure = ClaimFigure.of(claim, rates.findAll(sheet.id()), grade, settings, sheet.today());
        return ClaimFigure.due(figure, ClaimFigure.late(claim, figure, grade, settings, sheet.today()));
    }
}

package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Settles the claim and fixes its due: its amount times the rate of its person's grade in force on its date, in force on the
 * claim's date, divided by 100 and rounded to two decimals as the claim rounds (by the sheet's policy date), or
 * for a claim a statement bound its bound figure; with the late charge it has gained on that figure
 * by the sheet's today frozen in on top, and at least the floor in force on its date (applied last).
 * With no such rate the due is not fixed. The user who opened the claim, or last edited it, may not settle it.
 * When a clerk (any user not made a steward) settles a claim whose due is above the sheet's clerk
 * limit, it goes to PENDING instead, its due left live until a steward approves or rejects it; a
 * due of n/a is within any limit. Before the clerk limit is considered, a claim whose due is above
 * the proof threshold in force on its date is refused while it has no proofs, or none of a kind the proof kinds list. Only a role the
 * permission matrix allows may settle.
 */
public class SettleClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settingsStore, HttpServletRequest request)
            throws InvalidClaimStatusException, SelfSettleException {
        settle(sheet, claim, Audit.user(request), people, rates, roles, settingsStore);
        claim.setSettleBatch(null); // settled one at a time: in no batch
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
        Person person = people.find(sheet.id(), claim.getPersonId());
        Settings settings = settingsStore.find(sheet.id());
        String role = roles.roleOf(sheet.id(), user);
        if (!settings.allows("settle", role)) {
            throw new InvalidClaimStatusException("A " + role + " may not settle a claim");
        }
        String grade = person.gradeOn(claim.getDate());
        BigDecimal figure = claim.isStated() ? claim.getStatedDue()
                : ClaimFigure.live(claim, rates.findAll(sheet.id()), grade, settings, sheet.today());
        // The late charge is read while the claim is still OPEN.
        BigDecimal late = ClaimFigure.late(claim, figure, grade, settings, sheet.today());
        if (claim.isDeferredOn(sheet.today())) {
            throw new InvalidClaimStatusException("A claim deferred until " + claim.getDeferredUntil() + " cannot be settled");
        }
        if (!claim.getStatus().canMoveTo(ClaimStatus.SETTLED)) {
            throw new InvalidClaimStatusException(claim.getStatus(), ClaimStatus.SETTLED);
        }
        BigDecimal due = ClaimFigure.floored(claim, figure, late, settings);
        if (!judged) judge(claim, user, due, grade, settings);
        if (!settings.withinClerkLimit(due, grade, sheet.today()) && Role.CLERK.equals(role)) {
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
        BigDecimal figure = claim.isStated() ? claim.getStatedDue()
                : ClaimFigure.live(claim, rates.findAll(sheet.id()), grade, settings, sheet.today());
        return ClaimFigure.floored(claim, figure, ClaimFigure.late(claim, figure, grade, settings, sheet.today()), settings);
    }
}

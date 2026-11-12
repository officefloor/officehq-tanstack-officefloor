package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Freezes the SETTLED claim again at the due it would have today as an OPEN claim bound by no
 *  statement: its person's grade rate in force on its date, rounded as the sheet's policy date has
 *  it, plus the late charge it would have now, and at least the sheet's floor (none for a correction).
 *  Stores it and logs a SETTLED line, by the user; refused when another user moved it on first. */
public class ApplyResettle {
    public void apply(@Val ResettleInput input, @Val Claim claim, ClaimStore store, ClaimLogStore log,
            PersonStore people, RateStore rates, SettingStore settings, SheetStore sheets,
            Out<Resettlement> resettlement) throws InvalidSheetException {
        if (!store.move(claim, ClaimStatus.SETTLED, ClaimStatus.SETTLED)) {
            throw new InvalidSheetException("Only a SETTLED claim can be re-settled");
        }
        String sheetId = claim.getSheetId();
        Claim open = new Claim(claim.getId(), sheetId, claim.getPersonId(), claim.getAmount(), claim.getDate(),
                claim.getNote(), ClaimStatus.OPEN, claim.getNumber(), null, claim.getOpenedBy(), false, null,
                claim.getCorrects(), claim.getDueBy());
        Person person = people.find(sheetId, claim.getPersonId());
        BigDecimal pct = person == null ? null : rates.forClaim(claim).find(sheetId, person.gradeOn(claim.getDate()), claim.getDate());
        BigDecimal now = Lateness.of(sheetId, settings, sheets, store, people).due(open, pct);
        BigDecimal old = claim.getSettledDue();
        claim.setSettledDue(now);
        store.save(claim);
        log.record(claim, input.user());
        resettlement.set(new Resettlement(old, now));
    }
}

package net.officefloor.hq.app.paste;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDueNow;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.LateEntries;
import net.officefloor.hq.app.claim.NewDueBy;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds a DRAFT claim per pasted line, in order, each as the claim form's add does: due-by fixed from its date and
 * its person's grade's terms, its DRAFT status logged, and recorded as CLAIM_ADDED (with its reference, if any) (and LATE_ENTRY when its period is soft-closed).
 */
public class SavePastedClaims {
    public void save(@Val PastedLines pasted, @RequestParam("user") String user, SheetRepository sheets,
            SettingRepository settings, HolidayRepository holidays, PersonRepository people, RateRepository rates,
            ClaimRepository claims, ClaimLogRepository logs, Audit audit, LateEntries lateEntries, Out<PasteResult> result) {
        int added = 0;
        for (PastedLine line : pasted.lines()) {
            Person person = line.person();
            LocalDate date = line.input().date() != null ? line.input().date() : sheets.entryDate(person.sheetId());
            LocalDate dueBy = NewDueBy.of(date, person, settings, holidays);
            Claim claim = claims.save(new Claim(null, person.sheetId(), person.id(), line.input().amount(), date,
                    line.input().note(), ClaimStatus.DRAFT, null, null, null, false, null, line.input().corrects(),
                    dueBy));
            BigDecimal due = ClaimDueNow.of(claim, sheets, people, rates, settings, claims);
            logs.append(claim.sheetId(), new ClaimLogLine(claim.id(), claim.status(), user,
                    sheets.today(claim.sheetId()), due == null ? "n/a" : due.setScale(2).toPlainString()));
            claims.saveRef(claim, line.input().ref());
            audit.record("CLAIM_ADDED sheet=" + claim.sheetId() + " by=" + user + " person=" + person.name()
                    + " amount=" + claim.amount().setScale(2).toPlainString() + " date=" + claim.date()
                    + Claim.auditRef(line.input().ref()));
            lateEntries.mark(claim, person, user);
            added++;
        }
        result.set(new PasteResult(added));
    }
}

package net.officefloor.hq.app.paste;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.Corrections;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a DRAFT claim per pasted line, in order, to the person it names (compared as names are).
 *  Every line is first checked as {@link net.officefloor.hq.app.claim.AddClaim} checks a claim (its
 *  person on the sheet, not retired before its date, its date outside a closed period), so a refused
 *  line adds nothing; then each is added, logged and recorded as CLAIM_ADDED. */
public class PasteClaims {
    private record Pending(Person person, ValidClaim claim, LocalDate date, String corrects) {
    }

    public void paste(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val PasteLines lines,
            PersonRepository people, SheetRepository sheets, ClaimRepository claims,
            PeriodRepository periods, SettingRepository settings, ClaimLogRepository log, Audit audit,
            Out<PasteResult> result) throws PeriodClosedException, InvalidClaimException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        List<Pending> pending = new ArrayList<>();
        for (PasteLine line : lines.lines()) {
            Person person = people.findByName(id, line.name());
            if (person == null) {
                throw new InvalidClaimException("Not on the sheet: " + line.name());
            }
            ValidClaim request = line.claim();
            LocalDate date = request.date() != null ? request.date() : settings.entryDate(id);
            if (person.retiredBefore(date)) {
                throw new InvalidClaimException("The person is retired from " + person.getRetiredFrom());
            }
            periods.requireOpen(id, date.toString());
            String corrects = Corrections.check(request, id, person.getId(), claims);
            pending.add(new Pending(person, request, date, corrects));
        }
        for (Pending p : pending) {
            // The due-by is fixed now, by the terms of the grade in force on its date, as they stood on its date.
            LocalDate dueBy = p.date().plusDays(settings.terms(id, p.person(), p.date().toString()));
            Claim claim = claims.add(id, p.person().getId(), p.claim().amount(), p.date(),
                    p.claim().note(), p.corrects(), dueBy, user);
            if (periods.softClosedOn(id, claim.getDate())) claims.markLateEntry(claim);
            log.record(claim, user);
            audit.record("CLAIM_ADDED sheet=" + id + " by=" + user + " person=" + p.person().getName()
                    + " amount=" + claim.getAmount() + " date=" + claim.getDate());
            if (claim.isLateEntry()) {
                audit.record("LATE_ENTRY sheet=" + id + " by=" + user + " person=" + p.person().getName()
                        + " amount=" + claim.getAmount() + " date=" + claim.getDate());
            }
        }
        result.set(new PasteResult(pending.size()));
    }
}

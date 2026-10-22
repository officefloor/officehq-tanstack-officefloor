package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Split an amount off the loaded claim: only an OPEN claim not deferred on the sheet's today, and only an amount
 *  less than the claim's. The claim keeps the rest of its amount and loses any figure a statement bound it to; a new
 *  OPEN claim with the split-off amount, the same person, date and note, the next number, opened by the user, its
 *  due-by fixed now from the terms in force on its date (as a new claim's is) and the claim's number as its
 *  split-from is stored, its OPEN line starting its status log. A split another user's change got to first is
 *  refused. */
public class SaveSplit {
    public void save(@Val SplitInput input, @Val Claim loaded, SheetRepository sheets, SettingRepository settings,
            PersonRepository people, ClaimRepository repository, ClaimLogRepository log, Out<Split> split,
            Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException, InvalidClaimException {
        if (loaded.status() != ClaimStatus.OPEN) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim cannot be split; only an OPEN one can.");
        }
        var today = sheets.today(loaded.sheetId());
        if (loaded.deferredOn(today)) {
            throw new ClaimStatusNotAllowedException("This claim is deferred until " + loaded.deferredUntil()
                    + "; it cannot be split.");
        }
        if (input.amount().compareTo(loaded.amount()) >= 0) {
            throw new InvalidClaimException("The amount to split off must be less than the claim's "
                    + loaded.amount().toPlainString() + ".");
        }
        var rest = loaded.amount().subtract(input.amount());
        if (!repository.splitRest(loaded, rest)) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        var person = people.find(loaded.sheetId(), loaded.personId());
        var dueBy = loaded.date().plusDays(person.terms() != null ? person.terms()
                : settings.termsFor(loaded.sheetId(), person.grades().on(loaded.date()), loaded.date()));
        Claim inserted = repository.insert(new Claim(0, loaded.sheetId(), loaded.personId(), input.amount(),
                loaded.date(), loaded.note(), ClaimStatus.DRAFT, null, null, null, false, null, null, dueBy, null, 0,
                loaded.number(), false));
        String number = repository.nextNumber(loaded.sheetId(), loaded.date());
        repository.changeStatus(inserted, ClaimStatus.OPEN, number, null, input.user());
        log.record(inserted, input.user());
        Claim original = new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), rest, loaded.date(),
                loaded.note(), loaded.status(), loaded.number(), loaded.settledDue(), loaded.openedBy(), false, null,
                loaded.corrects(), loaded.dueBy(), loaded.deferredUntil(), loaded.proofs(), loaded.splitFrom(), loaded.lateEntry());
        Claim added = repository.find(loaded.sheetId(), inserted.id());
        split.set(new Split(original, added));
        saved.set(ClaimResponse.from(added, List.of(), today));
    }
}

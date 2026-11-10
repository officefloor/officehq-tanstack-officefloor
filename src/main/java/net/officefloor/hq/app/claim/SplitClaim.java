package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Splits an amount off an OPEN claim: the claim keeps the rest, unbound from any statement, and a new
 * OPEN claim with the amount split off, its date, note and tags, opened by the signed-in user and its
 * due-by fixed now from the terms in force on its date (counted in business days, skipping weekends and the holidays on the sheet now), is made (a late entry when that due-by is
 * inside a soft-closed period). Only a role the permission matrix
 * allows to open a claim may split one (the new claim is theirs to open).
 */
public class SplitClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, @Val SplitAmount split, RoleRepository roles,
            SettingsRepository settingsRepository, PersonRepository people, PeriodRepository periods, HolidayRepository holidays, HttpServletRequest request,
            Out<Split> splitOut) throws InvalidClaimStatusException, InvalidClaimException {
        String user = Audit.user(request);
        String role = roles.roleOf(sheet.id(), user);
        Settings settings = settingsRepository.find(sheet.id());
        if (!settings.allows("open", role)) {
            throw new InvalidClaimStatusException("A " + role + " may not split a claim");
        }
        claim.split(split.amount(), sheet.today());
        Person person = people.find(sheet.id(), claim.getPersonId());
        Claim made = new Claim(null, sheet.id(), claim.getPersonId(), split.amount(), claim.getDate(),
                claim.getNote(), ClaimStatus.OPEN, null, null, user);
        made.setSplitFrom(claim.getNumber());
        made.setTags(claim.getTags());
        made.fixDueBy(settings.termsFor(person, claim.getDate()), holidays.dates(sheet.id()));
        made.setLateEntry(periods.softClosedOn(sheet.id(), made.getDueBy()) != null);
        splitOut.set(new Split(made));
    }
}

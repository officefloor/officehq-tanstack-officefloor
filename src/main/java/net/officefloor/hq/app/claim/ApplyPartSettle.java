package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.history.AsOf;
import jakarta.persistence.EntityManager;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Settles the claim a part-settle has just split off, as the settle button does (PENDING when the clerk limit says
 *  so), by the signed-in user; its four-eyes and proof rules were judged on the original. Notes the sheet's figures
 *  between the split and the settle first, for the audit. */
public class ApplyPartSettle {
    public PartSettle apply(@Parameter Split split, @Val Sheet sheet, ClaimRepository repository,
            EntityManager entityManager, SheetRepository sheets, PersonRepository people, RateRepository rates,
            PersonGradeRepository grades, AsOf asOf, PeriodRepository periods, RoleRepository roles, ClaimLogRepository log,
            VoidReasonRepository voidReasons, @RequestHeader(name = "X-User", required = false) String userHeader)
            throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
        Rollup between = Rollup.of(sheet.getId(), sheets, people, repository, rates, grades, asOf);
        Claim made = split.made();
        String splitNumber = made.getNumber();
        String number = ApplyClaimStatus.run("settle", made, repository, entityManager, people, rates, grades, periods,
                sheet, roles, log, voidReasons, userHeader, null, null, false, true);
        ClaimRules.log(made, sheet, log, people, rates, grades, AuditUser.decode(userHeader));
        return new PartSettle(split, between, splitNumber, number);
    }
}

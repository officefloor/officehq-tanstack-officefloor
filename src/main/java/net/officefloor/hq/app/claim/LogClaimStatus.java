package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Writes the status the claim has just entered (DRAFT when it was added) to its status log: who made the change,
 * the sheet's today and the claim's due as the screen works it out now (n/a when it has none). */
public class LogClaimStatus {
    public void log(@Val Claim claim, @Val Sheet sheet, ClaimLogRepository log, PersonRepository people,
            RateRepository rates, PersonGradeRepository grades,
            @RequestHeader(name = "X-User", required = false) String user) {
        ClaimRules.log(claim, sheet, log, people, rates, grades, AuditUser.decode(user));
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.asof.AsOfState;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** A claim's due as it stood at the end of a date, as the as-of view works it out (the sheet put as it stood then,
 *  inside a savepoint always rolled back, so nothing is written). */
@Service
public class ClaimDueAsOf {

    /** The claim's due then (null for n/a); existed is false when the claim was not on the sheet then. */
    public record Then(boolean existed, BigDecimal due) {
    }

    private final AsOfState state;
    private final Claims claims;
    private final People people;
    private final Rates rates;
    private final Settings settings;
    private final SheetClock clock;
    private final TransactionTemplate transaction;

    public ClaimDueAsOf(AsOfState state, Claims claims, People people, Rates rates, Settings settings,
            SheetClock clock, PlatformTransactionManager transactions) {
        this.state = state;
        this.claims = claims;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    public Then on(String sheet, long id, LocalDate date) {
        return transaction.execute((status) -> {
            status.setRollbackOnly();
            state.show(sheet, date);
            Claim claim = claims.find(sheet, id);
            Person person = claim == null ? null : people.find(sheet, claim.person());
            if (person == null) {
                return new Then(false, null);
            }
            return new Then(true, claim.due(rates.list(sheet), person.gradeOn(claim.date()), settings.floor(sheet),
                    settings.late(sheet, clock.today(sheet))));
        });
    }
}

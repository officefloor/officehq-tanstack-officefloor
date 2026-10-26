package net.officefloor.hq.app.plan;

/** A plan: its person, amount (two decimals), start, every N days or a day of the month (the other null), end and how
 *  many claims it has made so far. */
public record PlanResponse(long id, long personId, String amount, String start, Integer every, Integer day, String end,
        int generated) {

    static PlanResponse of(Plan p) {
        return new PlanResponse(p.id(), p.personId(), p.amount().toPlainString(), p.start().toString(), p.every(), p.day(),
                p.end().toString(), p.generated());
    }
}

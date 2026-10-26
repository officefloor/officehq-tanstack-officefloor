package net.officefloor.hq.app.plan;

/** A plan: its person, amount (two decimals), start, every N days, end and how many claims it has made so far. */
public record PlanResponse(long id, long personId, String amount, String start, int every, String end, int generated) {

    static PlanResponse of(Plan p) {
        return new PlanResponse(p.id(), p.personId(), p.amount().toPlainString(), p.start().toString(), p.every(),
                p.end().toString(), p.generated());
    }
}

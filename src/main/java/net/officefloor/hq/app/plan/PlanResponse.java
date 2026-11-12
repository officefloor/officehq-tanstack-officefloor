package net.officefloor.hq.app.plan;

/** A plan as the front end reads it: the amount with two decimals, dates yyyy-MM-dd. */
public record PlanResponse(long id, long personId, String amount, String start, int every, String end,
        int generated) {

    public static PlanResponse from(Plan p) {
        return new PlanResponse(p.id(), p.personId(), p.amount().toPlainString(), p.start().toString(), p.every(),
                p.end().toString(), p.generated());
    }
}

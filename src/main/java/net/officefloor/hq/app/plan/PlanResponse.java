package net.officefloor.hq.app.plan;

/** A plan as the screen reads it: the amount with two decimals, dates as yyyy-mm-dd. */
public record PlanResponse(long id, long personId, String amount, String start, int every, String end, int generated) {
    public static PlanResponse from(Plan p) {
        return new PlanResponse(p.id(), p.personId(), p.amount().setScale(2).toPlainString(), p.start().toString(),
                p.every(), p.end().toString(), p.generated());
    }
}

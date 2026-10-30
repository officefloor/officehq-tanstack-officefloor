package net.officefloor.hq.app.plan;

/** A plan as the screen shows it (amount with two decimals, dates as yyyy-mm-dd). */
public record PlanResponse(String id, String personId, String amount, String start, int every, String end, int generated) {
    public static PlanResponse from(Plan p) {
        return new PlanResponse(String.valueOf(p.id()), String.valueOf(p.personId()), p.amount().toPlainString(), p.start().toString(),
                p.every(), p.end().toString(), p.generated());
    }
}

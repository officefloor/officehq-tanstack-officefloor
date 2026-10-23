package net.officefloor.hq.app.plan;

/** A plan as the screen gets it: whose it is, its amount (two decimals), dates (yyyy-mm-dd), every N days or the day of
 *  the month it runs on monthly (the other null), and how many claims it has made so far. */
public record PlanResponse(long id, long personId, String amount, String start, Integer every, Integer day, String end,
        int generated) {
    public static PlanResponse from(Plan p) {
        return new PlanResponse(p.id(), p.personId(), p.amount().setScale(2).toPlainString(), p.start().toString(),
                p.every(), p.day(), p.end().toString(), p.generated());
    }
}

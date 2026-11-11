package net.officefloor.hq.app.plan;

/** A plan, as the screen reads it: its person's id, amount with two decimals and how many claims it has made. */
public record PlanResponse(String id, String personId, String amount, String start, Integer every, Integer day, String end,
        int generated) {
    public static PlanResponse from(Plan p) {
        return new PlanResponse(String.valueOf(p.id()), String.valueOf(p.personId()), p.amount().toPlainString(),
                p.start().toString(), p.every(), p.day(), p.end().toString(), p.generated());
    }
}

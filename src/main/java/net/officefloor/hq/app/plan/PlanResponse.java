package net.officefloor.hq.app.plan;

/** A plan as the screens show it: amounts with two decimals, dates as yyyy-mm-dd. */
public record PlanResponse(long id, long personId, String amount, String start, Integer every, Integer day, String end,
        int generated) {
    static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.id(), plan.personId(), plan.amount().toPlainString(), plan.start().toString(),
                plan.every(), plan.day(), plan.end().toString(), plan.generated());
    }
}

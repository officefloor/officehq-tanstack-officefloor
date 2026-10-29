package net.officefloor.hq.app.plan;

/** A plan as the screen shows it: the amount with two decimals, the dates yyyy-mm-dd and how many claims it has made so far. */
public record PlanResponse(long id, long personId, String amount, String start, int every, String end, int generated, int day) {

    public static PlanResponse from(Plan plan, int generated) {
        return new PlanResponse(plan.id(), plan.personId(), plan.amount().toPlainString(), plan.start().toString(),
                plan.every(), plan.end().toString(), generated, plan.day());
    }
}

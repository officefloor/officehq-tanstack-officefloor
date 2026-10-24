package net.officefloor.hq.app.plan;

/** A plan as the screen shows it: its person, amount (two decimals), start, every N days or day of the month (the other
 *  null), end and how many claims it has made so far. */
public record PlanResponse(Long id, Long personId, String amount, String start, Integer every, Integer day, String end,
        long generated) {

    public static PlanResponse of(Plan plan, PlanClaimRepository made) {
        return new PlanResponse(plan.getId(), plan.getPersonId(), plan.getAmount().setScale(2).toPlainString(),
                plan.getStart().toString(), plan.getEvery(), plan.getDay(),
                plan.getEnd().toString(), made.countByPlanId(plan.getId()));
    }
}

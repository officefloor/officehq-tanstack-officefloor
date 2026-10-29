package net.officefloor.hq.app.dashboard;

/** The steward's dashboard counts; each null for a user who is not a steward. */
public record DashboardResponse(Integer pending, Integer review, Integer overdue) {

    public static final DashboardResponse NONE = new DashboardResponse(null, null, null);
}

package net.officefloor.hq.app;

import java.time.LocalDate;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/dashboard/overdue} — how many SENT invoices are overdue: those that have been sent
 * (DRAFT and PAID do not count) and whose due date is before the dashboard's fixed reference date.
 * "Overdue" is measured against the seeded {@link AppClock} "as of" date so the count is
 * deterministic; with no clock set it falls back to the real current date. Wired by
 * {@code officefloor/rest/api/dashboard/overdue.GET.yml}.
 */
public class DashboardOverdueGetLogic {

    public void service(InvoiceRepository invoices, AppClockRepository clocks,
            ObjectResponse<OverdueView> response) {
        LocalDate asOf = clocks.findById(1L).map(AppClock::getAsOf).orElse(LocalDate.now());
        long overdueCount = invoices.findAll().stream()
                .filter(i -> "SENT".equals(i.getStatus()))
                .filter(i -> i.getDueDate() != null && i.getDueDate().isBefore(asOf))
                .count();
        response.send(new OverdueView(overdueCount));
    }

    /** How many sent invoices are overdue as of the reference date. */
    public static class OverdueView {
        private final long overdueCount;

        public OverdueView(long overdueCount) {
            this.overdueCount = overdueCount;
        }

        public long getOverdueCount() {
            return overdueCount;
        }
    }
}

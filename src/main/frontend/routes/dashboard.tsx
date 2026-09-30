import { createFileRoute } from '@tanstack/react-router';
import { DashboardSummary } from '../features/dashboard/DashboardSummary';

// The Dashboard page: one new file under routes/ (CLAUDE.md rule 1). The route tree is generated
// from this directory. A flat page with no detail views, so a single route file (no layout). The
// page composes the feature's summary, which queries for itself.
export const Route = createFileRoute('/dashboard')({
  component: DashboardPage,
});

function DashboardPage() {
  return (
    <section data-testid="dashboard-page">
      <DashboardSummary />
    </section>
  );
}

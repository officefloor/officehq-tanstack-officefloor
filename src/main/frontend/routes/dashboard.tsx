import { createFileRoute } from '@tanstack/react-router';
import { Dashboard } from '../features/dashboard/Dashboard';

// The dashboard page: one new file under routes/. A leaf page with no detail views, so it is a flat
// route composing its own feature's summary, which queries for itself.
export const Route = createFileRoute('/dashboard')({
  component: DashboardPage,
});

function DashboardPage() {
  return (
    <section data-testid="dashboard-page">
      <h1>Dashboard</h1>
      <Dashboard />
    </section>
  );
}

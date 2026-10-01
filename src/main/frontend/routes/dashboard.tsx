import { createFileRoute } from '@tanstack/react-router';
import { Dashboard } from '../features/dashboard/Dashboard';

// The Dashboard page: one new file under routes/. The route table is generated from this directory.
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

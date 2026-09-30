import { createFileRoute } from '@tanstack/react-router';
import { Dashboard } from '../features/dashboard/Dashboard';

// The /dashboard home screen — one new file under routes/; the route tree is generated from this
// directory. Its nav link is its own file (features/dashboard/nav.slot.tsx).
export const Route = createFileRoute('/dashboard')({
  component: Dashboard,
});

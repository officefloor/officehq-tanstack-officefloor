import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// The dashboard's presence in the nav bar — one new file, the shell is not touched (see
// features/home/nav.slot.tsx for the worked example).
export const contribution = AppNav.fill({
  order: 5,
  Component: () => (
    <Link to="/dashboard" data-testid="nav-dashboard">
      Dashboard
    </Link>
  ),
});

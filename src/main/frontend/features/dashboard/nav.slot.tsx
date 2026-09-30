import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// The dashboard's presence in the shell nav bar — its own file, filling the app.nav slot. Ordered
// first so the home screen leads the nav; nothing existing is edited to add it.
export const contribution = AppNav.fill({
  order: 5,
  Component: () => (
    <Link to="/dashboard" data-testid="nav-dashboard">
      Dashboard
    </Link>
  ),
});

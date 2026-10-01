import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// The dashboard's presence in the nav bar — its own file filling the shared nav region, so the shell
// is not touched to add it (CLAUDE.md rule 1).
export const contribution = AppNav.fill({
  order: 5,
  Component: () => (
    <Link to="/dashboard" data-testid="nav-dashboard">
      Dashboard
    </Link>
  ),
});

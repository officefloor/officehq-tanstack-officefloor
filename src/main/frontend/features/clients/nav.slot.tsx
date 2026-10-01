import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// Clients' presence in the nav bar — its own file, filling the shared nav region. The shell is not
// touched to add it (CLAUDE.md rule 1).
export const contribution = AppNav.fill({
  order: 10,
  Component: () => (
    <Link to="/clients" data-testid="nav-clients">
      Clients
    </Link>
  ),
});

import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// Clients' presence in the nav bar — one new file, the shell is not touched (see
// features/home/nav.slot.tsx for the worked example).
export const contribution = AppNav.fill({
  order: 10,
  Component: () => (
    <Link to="/clients" data-testid="nav-clients">
      Clients
    </Link>
  ),
});

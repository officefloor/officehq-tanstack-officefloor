import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// Clients' presence in the shell nav bar — its own file, filling the app.nav slot. The shell lists
// no pages; adding this one is a new file, nothing edited.
export const contribution = AppNav.fill({
  order: 10,
  Component: () => (
    <Link to="/clients" data-testid="nav-clients">
      Clients
    </Link>
  ),
});

import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// Invoices' presence in the nav bar — its own file, filling the shared nav region. The shell is not
// touched to add it (CLAUDE.md rule 1). Links to the one page that lists every invoice.
export const contribution = AppNav.fill({
  order: 30,
  Component: () => (
    <Link to="/invoices" data-testid="nav-invoices">
      Invoices
    </Link>
  ),
});

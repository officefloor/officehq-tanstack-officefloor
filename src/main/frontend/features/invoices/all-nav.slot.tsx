import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// The all-invoices page's presence in the shell nav — its own file (CLAUDE.md rule 1). The shell
// lists no pages; it renders whatever fills app.nav.
export const contribution = AppNav.fill({
  order: 25,
  Component: () => (
    <Link to="/invoices" data-testid="nav-invoices">
      Invoices
    </Link>
  ),
});

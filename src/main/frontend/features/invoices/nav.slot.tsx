import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// Invoices' presence in the shell nav bar — its own file, filling the app.nav slot. Adding the
// one-place all-invoices page is a new file, nothing edited. Carries data-testid="nav-invoices".
export const contribution = AppNav.fill({
  order: 30,
  Component: () => (
    <Link to="/invoices" data-testid="nav-invoices">
      Invoices
    </Link>
  ),
});

import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// Invoices' presence in the nav bar — one new file, the shell is not touched (see
// features/home/nav.slot.tsx for the worked example).
export const contribution = AppNav.fill({
  order: 30,
  Component: () => (
    <Link to="/invoices" data-testid="nav-invoices">
      Invoices
    </Link>
  ),
});

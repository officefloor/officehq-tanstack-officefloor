import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// Projects' presence in the nav bar — one new file, the shell is not touched (see
// features/home/nav.slot.tsx for the worked example).
export const contribution = AppNav.fill({
  order: 20,
  Component: () => (
    <Link to="/projects" data-testid="nav-projects">
      Projects
    </Link>
  ),
});

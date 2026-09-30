import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// The Projects page's presence in the shell nav — its own file (CLAUDE.md rule 1). The shell lists
// no pages; it renders whatever fills app.nav.
export const contribution = AppNav.fill({
  order: 20,
  Component: () => (
    <Link to="/projects" data-testid="nav-projects">
      Jobs
    </Link>
  ),
});

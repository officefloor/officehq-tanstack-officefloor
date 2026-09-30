import { Link } from '@tanstack/react-router';
import { AppNav } from '../../slots/defs/appNav';

// Projects' presence in the shell nav bar — its own file, filling the app.nav slot. Adding this
// page is a new file, nothing edited.
export const contribution = AppNav.fill({
  order: 20,
  Component: () => (
    <Link to="/projects" data-testid="nav-projects">
      Projects
    </Link>
  ),
});

import { AppNav } from '../../slots/defs/appNav';
import { GlobalSearch } from './GlobalSearch';

// The global search box's presence in the shell — its own file, filling the app.nav slot so the one
// search box is available on every page (the home page included). The shell was written once and is
// not touched; adding this is a new file, nothing edited. A high order keeps it after the page links.
export const contribution = AppNav.fill({ order: 100, Component: GlobalSearch });

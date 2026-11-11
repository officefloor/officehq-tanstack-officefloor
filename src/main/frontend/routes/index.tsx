import { createFileRoute } from '@tanstack/react-router';
import { HomeMain } from '../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// The home page. A page is a file under routes/ — `createFileRoute('<its url>')` — with its nav
// link under features/. Its body is the home.main region, filled by features.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  if (!user || !sheet) return <p data-testid="home-empty">Nothing here yet.</p>;
  return <HomeMain.Slot />;
}

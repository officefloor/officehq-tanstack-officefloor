import { createFileRoute } from '@tanstack/react-router';
import { HomeMain } from '../slots/defs/homeMain';
import { forSlot } from '../slots/registry';

// The home page: its body is the home.main region, filled by features. A page is a file under
// routes/ — `createFileRoute('<its url>')` — with its nav link under features/.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  if (forSlot(HomeMain.id).length === 0) return <p data-testid="home-empty">Nothing here yet.</p>;
  return <HomeMain.Slot />;
}

import { createFileRoute } from '@tanstack/react-router';
import { HomeMain } from '../slots/defs/homeMain';

// The home page. A page is a file under routes/ — `createFileRoute('<its url>')` — with its nav
// link under features/. Its panels fill the home.main slot.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  return (
    <>
      <p data-testid="home-empty">Nothing here yet.</p>
      <HomeMain.Slot />
    </>
  );
}

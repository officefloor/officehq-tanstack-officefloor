import { createFileRoute } from '@tanstack/react-router';
import { HomeMain } from '../slots/defs/homeMain';

// The base home page. Its body is the home.main region: features fill it (features/**/*.slot.tsx)
// and this page never lists them. A page is ONE new file under routes/ — `createFileRoute('<its
// url>')` — plus its nav link under features/.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  return (
    <section data-testid="home-page">
      <p data-testid="home-empty">Nothing here yet.</p>
      <HomeMain.Slot />
    </section>
  );
}

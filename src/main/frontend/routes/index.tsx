import { createFileRoute } from '@tanstack/react-router';
import { HomeContent } from '../slots/defs/homeContent';

// The base home page: its body is the `home.content` region, which features fill — the page never
// lists what goes in it (CLAUDE.md rule 3). A page is ONE new file under routes/; a thing that
// lives on home is a `*.slot.tsx` file filling this region.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  return (
    <>
      <p data-testid="home-empty">Nothing here yet.</p>
      <HomeContent.Slot />
    </>
  );
}

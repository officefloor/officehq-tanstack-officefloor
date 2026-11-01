import { createFileRoute } from '@tanstack/react-router';
import { SheetPanels } from '../slots/defs/sheetPanels';
import { SignIn } from '../features/session/SignIn';
import { SheetPicker } from '../features/session/SheetPicker';
import { useCurrentSheet, useCurrentUser } from '../features/session/session';

// The one screen. Sign in, then load a sheet by id; the sheet's panels (filled from sheet.panels)
// show everything kept for that sheet.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();

  if (!user) return <SignIn />;

  return (
    <>
      <p>
        Signed in as <strong data-testid="current-user">{user}</strong>
      </p>
      <SheetPicker />
      {sheet ? (
        // Keyed by sheet so per-sheet form drafts never carry over to another sheet.
        <div key={sheet}>
          <SheetPanels.Slot sheet={sheet} user={user} />
        </div>
      ) : (
        <p data-testid="home-empty">Nothing here yet.</p>
      )}
    </>
  );
}

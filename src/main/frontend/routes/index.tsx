import { createFileRoute } from '@tanstack/react-router';
import { SheetPanels } from '../slots/defs/sheetPanels';
import { SignIn } from '../features/session/SignIn';
import { SheetPicker } from '../features/session/SheetPicker';
import { useCurrentSheet, useCurrentUser } from '../features/session/session';
import { notesQuery } from '../features/notes/notes';

// The one screen. Sign in, then load a sheet by id; the sheet's panels (filled from sheet.panels)
// show everything kept for that sheet.
// Loading a sheet re-reads its notes before the sheet shows as loaded, so the list on screen is the store's as it
// stands, never the list cached from an earlier load (or none at all while the read is still on its way).
export const Route = createFileRoute('/')({
  loaderDeps: ({ search }) => ({ sheet: (search as Record<string, unknown>).sheet, asof: (search as Record<string, unknown>).asof }),
  loader: async ({ context, deps }) => {
    if (typeof deps.sheet === 'string' && deps.sheet) {
      // A failed read shows through the notes panel's own query; it never stops the sheet loading.
      await context.queryClient.fetchQuery(notesQuery(deps.sheet)).catch(() => undefined);
    }
  },
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

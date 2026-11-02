import { createFileRoute } from '@tanstack/react-router';
import { SheetPanels } from '../slots/defs/sheetPanels';
import { SignIn } from '../features/session/SignIn';
import { SheetPicker } from '../features/session/SheetPicker';
import { useCurrentSheet, useCurrentUser } from '../features/session/session';
import { notesQuery } from '../features/notes/notes';
import { claimsKey, claimsPath, type Claim } from '../features/claims/claims';
import { peopleKey, peoplePath, type Person } from '../features/people/people';
import { getJson } from '../api/http';

// The one screen. Sign in, then load a sheet by id; the sheet's panels (filled from sheet.panels)
// show everything kept for that sheet.
// Loading a sheet re-reads its notes, people and claims before the sheet shows as loaded, so the lists on screen are
// the store's as they stand, never the lists cached from an earlier load (or none at all while a read is still on its way).
export const Route = createFileRoute('/')({
  loaderDeps: ({ search }) => ({ sheet: (search as Record<string, unknown>).sheet, asof: (search as Record<string, unknown>).asof }),
  loader: async ({ context, deps }) => {
    if (typeof deps.sheet === 'string' && deps.sheet) {
      // A failed read shows through the panel's own query; it never stops the sheet loading.
      const sheet = deps.sheet;
      await Promise.all([
        context.queryClient.fetchQuery(notesQuery(sheet)),
        context.queryClient.fetchQuery({ queryKey: peopleKey(sheet), queryFn: () => getJson<Person[]>(peoplePath(sheet)) }),
        context.queryClient.fetchQuery({ queryKey: claimsKey(sheet), queryFn: () => getJson<Claim[]>(claimsPath(sheet)) }),
      ].map((read) => read.catch(() => undefined)));
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

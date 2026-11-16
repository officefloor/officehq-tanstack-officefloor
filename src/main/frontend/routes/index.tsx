import { createFileRoute } from '@tanstack/react-router';
import { SignIn } from '../features/session/SignIn';
import { SheetPicker } from '../features/session/SheetPicker';
import { useSheet, useSheetId, useUser } from '../features/session/session';
import { SheetPage } from '../slots/defs/sheetPage';
import { usePeople } from '../features/people/people';
import { useClaims } from '../features/claims/claims';

// The one screen. Sign in first; then choose a sheet, whose content is the sheet.page region.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useUser();
  const [sheetId] = useSheetId();
  const sheet = useSheet(user === '' ? '' : sheetId);

  if (user === '') return <SignIn />;
  return (
    <>
      <p>
        Signed in as <span data-testid="current-user">{user}</span>
      </p>
      <SheetPicker />
      {sheet.isError && <p data-testid="sheet-load-error">Could not load sheet {sheetId}.</p>}
      {sheet.data ? (
        <section data-testid="sheet">
          <h2>
            Sheet <CurrentId sheetId={sheet.data.id} />
          </h2>
          <SheetPage.Slot sheetId={sheet.data.id} user={user} />
        </section>
      ) : (
        <p data-testid="home-empty">Nothing here yet.</p>
      )}
    </>
  );
}

/** The loaded sheet's id, shown only once its people and claims are in, so its rows are on screen with it. */
function CurrentId({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  if (people.isPending || claims.isPending) return null;
  return <span data-testid="sheet-current-id">{sheetId}</span>;
}

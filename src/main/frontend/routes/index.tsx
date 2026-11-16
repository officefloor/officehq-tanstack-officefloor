import { createFileRoute } from '@tanstack/react-router';
import { SignIn } from '../features/session/SignIn';
import { SheetPicker } from '../features/session/SheetPicker';
import { useSheet, useSheetId, useUser } from '../features/session/session';
import { SheetPage } from '../slots/defs/sheetPage';

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
            Sheet <span data-testid="sheet-current-id">{sheet.data.id}</span>
          </h2>
          <SheetPage.Slot sheetId={sheet.data.id} user={user} />
        </section>
      ) : (
        <p data-testid="home-empty">Nothing here yet.</p>
      )}
    </>
  );
}

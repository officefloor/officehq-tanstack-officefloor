import { useState } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { SignIn } from '../features/session/SignIn';
import { SheetLoader } from '../features/session/SheetLoader';
import { useSheetId, useUser } from '../features/session/session';
import { SheetBody } from '../slots/defs/sheetBody';

// The one screen. Sign in by name, then load a sheet by id; the loaded sheet's body is the
// sheet.body region, filled by features.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useUser();
  const [sheetId] = useSheetId();
  // Bumped on every load, so loading the same id again remounts the body for a fresh read.
  const [loads, setLoads] = useState(0);

  if (user === '') {
    return <SignIn />;
  }

  return (
    <>
      <p>
        Signed in as <strong data-testid="current-user">{user}</strong>
      </p>
      <SheetLoader onLoaded={() => setLoads((n) => n + 1)} />
      {sheetId === '' ? (
        <p data-testid="home-empty">Nothing here yet.</p>
      ) : (
        <section data-testid="sheet-body" key={`${sheetId}:${loads}`}>
          <SheetBody.Slot sheetId={sheetId} user={user} />
        </section>
      )}
    </>
  );
}

import { createFileRoute } from '@tanstack/react-router';
import { SignIn, useCurrentUser } from '../features/session/SignIn';
import { SheetPicker, useSheetId } from '../features/session/SheetPicker';

// The one screen. Sign in by name, then load a sheet by id; everything else is per sheet.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useCurrentUser();
  const sheet = useSheetId();
  return (
    <>
      <SignIn />
      {user && <SheetPicker />}
      {user && !sheet && <p data-testid="home-empty">Nothing here yet.</p>}
    </>
  );
}

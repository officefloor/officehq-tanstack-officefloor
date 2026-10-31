import { createFileRoute } from '@tanstack/react-router';
import { CurrentUser, SignIn } from '../features/session/SignIn';
import { useCurrentSheet, useCurrentUser } from '../features/session/useSession';
import { SheetPicker } from '../features/sheet/SheetPicker';
import { SheetPanels } from '../slots/defs/sheetPanels';

// The one screen. Sign in first; then load a sheet, whose content fills the sheet.panels slot.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();

  if (user === '') return <SignIn />;
  return (
    <>
      <CurrentUser user={user} />
      <SheetPicker />
      {sheet === '' ? (
        <p data-testid="home-empty">Nothing here yet.</p>
      ) : (
        // Keyed by sheet so loading another sheet starts its panels afresh.
        <SheetPanels.Slot key={sheet} user={user} sheet={sheet} />
      )}
    </>
  );
}

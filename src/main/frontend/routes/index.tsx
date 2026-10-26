import { createFileRoute } from '@tanstack/react-router';
import { useUser } from '../features/session/useUser';
import { SignIn } from '../features/session/SignIn';
import { CurrentUser } from '../features/session/CurrentUser';
import { SheetPicker } from '../features/sheet/SheetPicker';
import { ActiveSheet } from '../features/sheet/ActiveSheet';

// The one screen: sign in by name, then load a sheet by id. Everything else is kept per sheet and
// fills the sheet.panel slot (slots/defs/sheetPanel.ts).
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useUser();
  if (user === '') return <SignIn />;
  return (
    <>
      <CurrentUser />
      <SheetPicker />
      <ActiveSheet user={user} />
    </>
  );
}

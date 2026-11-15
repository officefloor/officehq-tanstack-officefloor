import { createFileRoute } from '@tanstack/react-router';
import { useUser } from '../features/session/session';
import { SignIn } from '../features/session/SignIn';
import { CurrentUser } from '../features/session/CurrentUser';
import { UserLocaleSelect } from '../features/session/UserLocaleSelect';
import { SheetPicker } from '../features/sheet/SheetPicker';
import { ActiveSheet } from '../features/sheet/ActiveSheet';

// The one screen. Sign in first; then choose a sheet, and everything else is on that sheet.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useUser();
  if (!user) return <SignIn />;
  return (
    <>
      <CurrentUser />
      <UserLocaleSelect />
      <SheetPicker />
      <ActiveSheet />
    </>
  );
}

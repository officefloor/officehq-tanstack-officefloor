import { createFileRoute } from '@tanstack/react-router';
import { SignIn } from '../features/session/SignIn';
import { UserBar } from '../features/session/UserBar';
import { useSheetId, useUser } from '../features/session/useSession';
import { SheetPicker } from '../features/sheet/SheetPicker';
import { SheetBody } from '../slots/defs/sheetBody';

// The one screen. Sign in first; then choose a sheet; then the sheet's features (the sheet.body
// slot) work on that sheet. A page is a file under routes/ — `createFileRoute('<its url>')`.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useUser();
  const [sheetId] = useSheetId();
  if (!user) {
    return <SignIn />;
  }
  return (
    <>
      <UserBar />
      <SheetPicker />
      {sheetId ? (
        <SheetBody.Slot key={sheetId} sheetId={sheetId} user={user} />
      ) : (
        <p data-testid="home-empty">Load a sheet to begin.</p>
      )}
    </>
  );
}

import { createFileRoute } from '@tanstack/react-router';
import { CurrentUser, SignIn } from '../features/session/SignIn';
import { useActiveSheet, useSignedInUser } from '../features/session/useSession';
import { SheetPicker } from '../features/sheet/SheetPicker';
import { SheetBody } from '../slots/defs/sheetBody';

// The one screen. Sign in first; then choose a sheet; the sheet's own features fill SheetBody.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useSignedInUser();
  const [sheetId] = useActiveSheet();
  if (!user) return <SignIn />;
  return (
    <>
      <CurrentUser />
      <SheetPicker />
      {sheetId && (
        <section data-testid="sheet-body" key={sheetId}>
          <SheetBody.Slot sheetId={sheetId} user={user} />
        </section>
      )}
    </>
  );
}

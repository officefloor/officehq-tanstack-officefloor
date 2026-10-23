import { createFileRoute } from '@tanstack/react-router';
import { LocaleFigures } from '../features/locale/LocaleFigures';
import { UserLocale } from '../features/locale/UserLocale';
import { useLocale } from '../features/locale/locale';
import { CurrentUser, SignIn } from '../features/session/SignIn';
import { useSheetId, useUser } from '../features/session/session';
import { SheetPicker } from '../features/sheet/SheetPicker';
import { SheetBody } from '../slots/defs/sheet';

// The one screen: sign in by name, choose a sheet by id, then everything kept per sheet.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useUser();
  const [sheetId] = useSheetId();
  const locale = useLocale(user ?? '');

  if (!user) {
    return (
      <>
        <SignIn />
        <p data-testid="home-empty">Nothing here yet.</p>
      </>
    );
  }
  // Every figure shown to the signed-in user is in their locale.
  return (
    <LocaleFigures locale={locale}>
      <CurrentUser user={user} />
      <UserLocale user={user} />
      <SheetPicker user={user} />
      {sheetId ? (
        <SheetBody.Slot key={sheetId} sheetId={sheetId} user={user} />
      ) : (
        <p data-testid="home-empty">Nothing here yet.</p>
      )}
    </LocaleFigures>
  );
}

import { createFileRoute } from '@tanstack/react-router';
import { LocaleFigures } from '../features/locale/LocaleFigures';
import { useLocale } from '../features/locale/useLocale';
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
  const locale = useLocale(user ?? '');
  if (!user) {
    return <SignIn />;
  }
  return (
    <>
      <UserBar />
      <SheetPicker />
      {sheetId ? (
        <LocaleFigures locale={locale.data?.locale ?? 'en'}>
          <SheetBody.Slot key={sheetId} sheetId={sheetId} user={user} />
        </LocaleFigures>
      ) : (
        <p data-testid="home-empty">Load a sheet to begin.</p>
      )}
    </>
  );
}

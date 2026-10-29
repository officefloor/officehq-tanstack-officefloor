import { createFileRoute } from '@tanstack/react-router';
import { useUser } from '../features/session/useUser';
import { SignIn } from '../features/session/SignIn';
import { CurrentUser } from '../features/session/CurrentUser';
import { SheetPicker } from '../features/sheet/SheetPicker';
import { ActiveSheet } from '../features/sheet/ActiveSheet';
import { useQuery } from '@tanstack/react-query';
import { LocaleFigures } from '../features/locale/LocaleFigures';
import { LocalePicker } from '../features/locale/LocalePicker';
import { localeQuery } from '../features/locale/localeQuery';

// The one screen: sign in by name, then load a sheet by id. Everything else is kept per sheet and
// fills the sheet.panel slot (slots/defs/sheetPanel.ts).
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useUser();
  if (user === '') return <SignIn />;
  return <SignedIn user={user} />;
}

// Every figure on the screen is shown under the signed-in user's locale (features/locale).
function SignedIn({ user }: { user: string }) {
  const locale = useQuery(localeQuery(user)).data ?? 'en';
  return (
    <LocaleFigures locale={locale}>
      <CurrentUser />
      <LocalePicker user={user} />
      <SheetPicker />
      <ActiveSheet user={user} />
    </LocaleFigures>
  );
}

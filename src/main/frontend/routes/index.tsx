import { createFileRoute } from '@tanstack/react-router';
import { asString, useSearchParam } from '../url/useSearchParam';
import { CurrentUser, SignIn } from '../features/session/SignIn';
import { SheetPicker } from '../features/session/SheetPicker';
import { SheetToday } from '../features/sheet/SheetToday';
import { RatePanel } from '../features/rates/RatePanel';
import { SettingsPanel } from '../features/settings/SettingsPanel';
import { PeriodPanel } from '../features/periods/PeriodPanel';
import { PersonForm } from '../features/people/PersonForm';
import { PeopleList } from '../features/people/PeopleList';

// The one screen. Signed out it shows only the sign-in form; signed in, the user and the sheet
// picker, and — once a sheet is loaded — that sheet.
export const Route = createFileRoute('/')({
  component: Home,
});

function Home() {
  const [user] = useSearchParam('user', asString);
  const [sheet] = useSearchParam('sheet', asString);
  if (user === '') return <SignIn />;
  return (
    <>
      <CurrentUser />
      <SheetPicker />
      {sheet !== '' && (
        <div key={sheet}>
          <SheetToday />
          <RatePanel />
          <SettingsPanel />
          <PeriodPanel />
          <PersonForm />
          <PeopleList />
        </div>
      )}
      {sheet === '' && <p data-testid="home-empty">Nothing here yet.</p>}
    </>
  );
}

import { createFileRoute } from '@tanstack/react-router';
import { asString, useSearchParam } from '../url/useSearchParam';
import { CurrentUser, SignIn } from '../features/session/SignIn';
import { LocaleControl } from '../features/locale/LocaleControl';
import { SheetPicker } from '../features/session/SheetPicker';
import { SheetToday } from '../features/sheet/SheetToday';
import { AsOfControl } from '../features/asof/AsOfControl';
import { PreviewControl } from '../features/preview/PreviewControl';
import { RatePanel } from '../features/rates/RatePanel';
import { HolidayPanel } from '../features/holidays/HolidayPanel';
import { GradePanel } from '../features/grades/GradePanel';
import { VoidReasonPanel } from '../features/voidreasons/VoidReasonPanel';
import { NotePanel } from '../features/notes/NotePanel';
import { PoolPanel } from '../features/pools/PoolPanel';
import { CrewPanel } from '../features/crews/CrewPanel';
import { TemplatePanel } from '../features/templates/TemplatePanel';
import { SettingsPanel } from '../features/settings/SettingsPanel';
import { PeriodPanel } from '../features/periods/PeriodPanel';
import { PersonForm } from '../features/people/PersonForm';
import { PeopleList } from '../features/people/PeopleList';
import { DuplicateClaims } from '../features/claims/DuplicateClaims';
import { SheetHeader } from '../features/header/SheetHeader';
import { StewardDashboard } from '../features/dashboard/StewardDashboard';
import { ConsolidatedFigures } from '../features/consolidated/ConsolidatedFigures';
import { TreeSearch } from '../features/search/TreeSearch';
import { AuditViewer } from '../features/audit/AuditViewer';
import { ActivityPanel } from '../features/audit/ActivityPanel';

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
      <LocaleControl />
      <SheetPicker />
      {sheet !== '' && (
        <div key={sheet}>
          <SheetHeader />
          <StewardDashboard />
          <SheetToday />
          <ConsolidatedFigures />
          <AsOfControl />
          <PreviewControl />
          <RatePanel />
          <GradePanel />
          <HolidayPanel />
          <VoidReasonPanel />
          <NotePanel />
          <TemplatePanel />
          <SettingsPanel />
          <PeriodPanel />
          <PersonForm />
          <PeopleList />
          <DuplicateClaims />
          <PoolPanel />
          <CrewPanel />
          <TreeSearch />
          <AuditViewer />
          <ActivityPanel />
        </div>
      )}
      {sheet === '' && <p data-testid="home-empty">Nothing here yet.</p>}
    </>
  );
}

import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClaimForm } from '../claims/ClaimForm';
import { BulkOpenButton, ClaimSelection } from '../claims/BulkOpen';
import { ClaimList } from '../claims/ClaimList';
import { countsLive, sumAmounts, useClaims } from '../claims/useClaims';
import { isOverdue } from '../claims/overdue';
import { claimDue, dueTotal, formatCents, showDue } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { useFloor, useLatePct, usePolicyDate, useTerms } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { StatementButton } from '../statements/StatementButton';
import { SummaryPanel } from '../summary/SummaryPanel';
import { HoldControl } from './HoldControl';
import { PatronControl } from './PatronControl';
import { QuickAdd } from './QuickAdd';
import { RetireControl } from './RetireControl';
import { downloadCsv } from './csv';
import { ShowRetired, useShowRetired } from './ShowRetired';
import { gradeOn, usePeople, type Person } from './usePeople';

// Every person on the sheet in the order they were added, with their name, grade (the one in force on the sheet's today), claimed (the
// sum of their OPEN and SETTLED claims), due-total (the
// sum of their counting claims' already-rounded dues; n/a when their grade has no rate today and no settled or stated claim keeps a figure),
// whether they are retired and from when, whether they are on hold (a held person's OPEN claims count
// in neither their figures nor the sheet's totals; their SETTLED claims still do), their overdue count and
// patrons (names, sorted) and backing (their patrons' due-totals added up, skipping n/a; 0.00 with no patrons),
// their overdue count and overdue-total (how many of their counting claims are overdue, and the sum of those claims' dues with their late charges, n/a adding nothing), their statements (the statement button, how many were taken, the last one's due-total and date) and their claims; then the
// sheet's claimed-total, due-total (the people's due-totals added up, skipping n/a), overdue count and overdue-total. Retired
// people have no row unless "show retired" is on, but their claims keep counting in every total.
// Then the summary panel, worked from these same figures. The export button downloads every person (retired too), in list
// order, as CSV: name, grade in force today, retired-from (or nothing) and due-total as shown. Claims ticked
// with their select checkbox are opened together with the bulk open button.
export function PeopleList() {
  const [sheet] = useSearchParam('sheet', asString);
  const [, setEditing] = useSearchParam('editPerson', asString);
  const people = usePeople(sheet);
  const [showRetired] = useShowRetired();
  const listed = showRetired ? people : people.filter((p) => !p.retired);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const terms = useTerms(sheet);
  const late = { pct: useLatePct(sheet), terms, today };
  const policy = usePolicyDate(sheet);
  const heldOf = new Map(people.map((p) => [p.id, p.held]));
  const live = claims.filter((c) => heldOf.has(c.personId) && countsLive(c, heldOf.get(c.personId)!));
  // A claim is worked at the grade in force on its date; the row shows the one in force today.
  const totalOf = (person: Person) =>
    dueTotal(
      live.filter((c) => c.personId === person.id),
      (date) => rateOn(rates, gradeOn(person, date), date),
      rateOn(rates, gradeOn(person, today), today),
      floor,
      late,
      policy,
    );
  const byId = new Map(people.map((p) => [p.id, p]));
  const backing = (p: (typeof people)[number]) =>
    formatCents(p.patronIds.reduce((sum, id) => {
      const patron = byId.get(id);
      const due = patron ? totalOf(patron) : null;
      return due === null ? sum : sum + due;
    }, 0n));

  const overdue = live.filter((c) => isOverdue(c, terms, today));
  const overdueOf = (personId: string) => overdue.filter((c) => c.personId === personId);
  const overdueTotal = (cs: typeof overdue) =>
    formatCents(cs.reduce((sum, c) => sum + (claimDue(c, rateOn(rates, gradeOn(byId.get(c.personId)!, c.date), c.date), floor, late, policy) ?? 0n), 0n));
  return (
    <ClaimSelection>
      <QuickAdd />
      <ShowRetired />
      <button
        type="button"
        data-testid="export-people"
        onClick={() =>
          downloadCsv(
            'people.csv',
            [['name', 'grade', 'retired-from', 'due-total'], ...people.map((p) => [p.name, gradeOn(p, today), p.retiredFrom, showDue(totalOf(p))])],
          )
        }
      >
        Export CSV
      </button>
      <BulkOpenButton />
      <table data-testid="people">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Claimed</th>
            <th>Due</th>
            <th>Patrons</th>
            <th>Backing</th>
            <th>Overdue</th>
            <th>Overdue due</th>
            <th />
            <th>Retired</th>
            <th>Retired from</th>
            <th>Hold</th>
            <th>Statements</th>
            <th>Last stated</th>
            <th>Stated on</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {listed.map((p) => {
            const own = claims.filter((c) => c.personId === p.id);
            return (
              <tr key={p.id} data-testid={`person-row-${p.id}`}>
                <td data-testid="person-name">{p.name}</td>
                <td data-testid="person-grade">{gradeOn(p, today)}</td>
                <td data-testid="person-claimed">{sumAmounts(live.filter((c) => c.personId === p.id))}</td>
                <td data-testid="person-due-total">{showDue(totalOf(p))}</td>
                <td data-testid="person-patrons">{p.patrons.join(', ')}</td>
                <td data-testid="person-backing">{backing(p)}</td>
                <td data-testid="person-overdue-count">{overdueOf(p.id).length}</td>
                <td data-testid="person-overdue-total">{overdueTotal(overdueOf(p.id))}</td>
                <td>
                  <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(p.id)}>
                    Edit
                  </button>{' '}
                  <StatementButton personId={p.id} />{' '}
                  <RetireControl personId={p.id} />{' '}
                  <HoldControl personId={p.id} held={p.held} />{' '}
                  <PatronControl personId={p.id} people={people} />
                </td>
                <td data-testid="person-retired">{p.retired ? 'yes' : 'no'}</td>
                <td data-testid="person-retired-from">{p.retiredFrom}</td>
                <td data-testid="person-hold">{p.held ? 'yes' : 'no'}</td>
                <td data-testid="person-statement-count">{p.statementCount}</td>
                <td data-testid="person-stated-total">{p.statedTotal}</td>
                <td data-testid="person-statement-date">{p.statementDate}</td>
                <td>
                  <ClaimList claims={own} rateOn={(date) => rateOn(rates, gradeOn(p, date), date)} />
                  <ClaimForm personId={p.id} claims={own} />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <p>
        Claimed total: <span data-testid="rollup-claimed-total">{sumAmounts(live)}</span>
      </p>
      <p>
        Due total: <span data-testid="rollup-due-total">{formatCents(people.reduce((sum, p) => {
          const due = totalOf(p);
          return due === null ? sum : sum + due;
        }, 0n))}</span>
      </p>
      <p>
        Overdue: <span data-testid="rollup-overdue-count">{overdue.length}</span>
      </p>
      <p>
        Overdue total: <span data-testid="rollup-overdue-total">{overdueTotal(overdue)}</span>
      </p>
      <SummaryPanel people={people} claims={claims} today={today} totalOf={totalOf} />
    </ClaimSelection>
  );
}

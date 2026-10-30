import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClaimForm } from '../claims/ClaimForm';
import { ClaimList } from '../claims/ClaimList';
import { countsLive, sumAmounts, useClaims } from '../claims/useClaims';
import { dueTotal, formatCents, showDue } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { useFloor } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { StatementButton } from '../statements/StatementButton';
import { HoldControl } from './HoldControl';
import { QuickAdd } from './QuickAdd';
import { RetireControl } from './RetireControl';
import { ShowRetired, useShowRetired } from './ShowRetired';
import { usePeople } from './usePeople';

// Every person on the sheet in the order they were added, with their name, grade, claimed (the
// sum of their OPEN and SETTLED claims), due-total (the
// sum of their counting claims' already-rounded dues; n/a when their grade has no rate today and no settled or stated claim keeps a figure),
// whether they are retired and from when, whether they are on hold (a held person's OPEN claims count
// in neither their figures nor the sheet's totals; their SETTLED claims still do), their statements (the statement button, how many were taken, the last one's due-total and date) and their claims; then the
// sheet's claimed-total and due-total (the people's due-totals added up, skipping n/a). Retired
// people have no row unless "show retired" is on, but their claims keep counting in every total.
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
  const heldOf = new Map(people.map((p) => [p.id, p.held]));
  const live = claims.filter((c) => heldOf.has(c.personId) && countsLive(c, heldOf.get(c.personId)!));
  const totalOf = (personId: string, grade: string) =>
    dueTotal(
      live.filter((c) => c.personId === personId),
      (date) => rateOn(rates, grade, date),
      rateOn(rates, grade, today),
      floor,
    );
  return (
    <>
      <QuickAdd />
      <ShowRetired />
      <table data-testid="people">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Claimed</th>
            <th>Due</th>
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
                <td data-testid="person-grade">{p.grade}</td>
                <td data-testid="person-claimed">{sumAmounts(live.filter((c) => c.personId === p.id))}</td>
                <td data-testid="person-due-total">{showDue(totalOf(p.id, p.grade))}</td>
                <td>
                  <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(p.id)}>
                    Edit
                  </button>{' '}
                  <StatementButton personId={p.id} />{' '}
                  <RetireControl personId={p.id} />{' '}
                  <HoldControl personId={p.id} held={p.held} />
                </td>
                <td data-testid="person-retired">{p.retired ? 'yes' : 'no'}</td>
                <td data-testid="person-retired-from">{p.retiredFrom}</td>
                <td data-testid="person-hold">{p.held ? 'yes' : 'no'}</td>
                <td data-testid="person-statement-count">{p.statementCount}</td>
                <td data-testid="person-stated-total">{p.statedTotal}</td>
                <td data-testid="person-statement-date">{p.statementDate}</td>
                <td>
                  <ClaimList claims={own} rateOn={(date) => rateOn(rates, p.grade, date)} />
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
          const due = totalOf(p.id, p.grade);
          return due === null ? sum : sum + due;
        }, 0n))}</span>
      </p>
    </>
  );
}

import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClaimForm } from '../claims/ClaimForm';
import { ClaimList } from '../claims/ClaimList';
import { counts, sumAmounts, useClaims } from '../claims/useClaims';
import { dueTotal, formatCents, showDue } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { useSheetToday } from '../sheet/useSheetToday';
import { StatementButton } from '../statements/StatementButton';
import { usePeople } from './usePeople';

// Every person on the sheet in the order they were added, with their name, grade, claimed (the
// sum of their OPEN and SETTLED claims), due-total (the
// sum of their counting claims' already-rounded dues; n/a when their grade has no rate today and no settled or stated claim keeps a figure),
// their statements (the statement button, how many were taken, the last one's due-total and date) and their claims; then the
// sheet's claimed-total and due-total (the people's due-totals added up, skipping n/a).
export function PeopleList() {
  const [sheet] = useSearchParam('sheet', asString);
  const [, setEditing] = useSearchParam('editPerson', asString);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const onSheet = new Set(people.map((p) => p.id));
  const totalOf = (personId: string, grade: string) =>
    dueTotal(
      claims.filter((c) => c.personId === personId && counts(c)),
      (date) => rateOn(rates, grade, date),
      rateOn(rates, grade, today),
    );
  return (
    <>
      <table data-testid="people">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Claimed</th>
            <th>Due</th>
            <th />
            <th>Statements</th>
            <th>Last stated</th>
            <th>Stated on</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {people.map((p) => {
            const own = claims.filter((c) => c.personId === p.id);
            return (
              <tr key={p.id} data-testid={`person-row-${p.id}`}>
                <td data-testid="person-name">{p.name}</td>
                <td data-testid="person-grade">{p.grade}</td>
                <td data-testid="person-claimed">{sumAmounts(own)}</td>
                <td data-testid="person-due-total">{showDue(totalOf(p.id, p.grade))}</td>
                <td>
                  <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(p.id)}>
                    Edit
                  </button>{' '}
                  <StatementButton personId={p.id} />
                </td>
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
        Claimed total: <span data-testid="rollup-claimed-total">{sumAmounts(claims.filter((c) => onSheet.has(c.personId)))}</span>
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

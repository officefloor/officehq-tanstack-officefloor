import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClaimForm } from '../claims/ClaimForm';
import { ClaimList } from '../claims/ClaimList';
import { sumAmounts, useClaims } from '../claims/useClaims';
import { dueCents, formatCents, showDue, toCents } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { useSheetToday } from '../sheet/useSheetToday';
import { usePeople } from './usePeople';

// Every person on the sheet in the order they were added, with their name, grade, claimed (the
// sum of their OPEN and SETTLED claims), due-total (claimed at their grade's rate in force on the sheet's today) and their claims; then the
// sheet's claimed-total and due-total (the people's due-totals added up, skipping n/a).
export function PeopleList() {
  const [sheet] = useSearchParam('sheet', asString);
  const [, setEditing] = useSearchParam('editPerson', asString);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const onSheet = new Set(people.map((p) => p.id));
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
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {people.map((p) => {
            const own = claims.filter((c) => c.personId === p.id);
            const rate = rateOn(rates, p.grade, today);
            return (
              <tr key={p.id} data-testid={`person-row-${p.id}`}>
                <td data-testid="person-name">{p.name}</td>
                <td data-testid="person-grade">{p.grade}</td>
                <td data-testid="person-claimed">{sumAmounts(own)}</td>
                <td data-testid="person-due-total">{showDue(dueCents(toCents(sumAmounts(own)), rate))}</td>
                <td>
                  <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(p.id)}>
                    Edit
                  </button>
                </td>
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
          const due = dueCents(toCents(sumAmounts(claims.filter((c) => c.personId === p.id))), rateOn(rates, p.grade, today));
          return due === null ? sum : sum + due;
        }, 0n))}</span>
      </p>
    </>
  );
}

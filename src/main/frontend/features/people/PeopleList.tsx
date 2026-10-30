import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClaimForm } from '../claims/ClaimForm';
import { ClaimList } from '../claims/ClaimList';
import { sumAmounts, useClaims } from '../claims/useClaims';
import { usePeople } from './usePeople';

// Every person on the sheet in the order they were added, with their name, grade, claimed (the
// sum of their claims) and their claims; then the sheet's claimed-total.
export function PeopleList() {
  const [sheet] = useSearchParam('sheet', asString);
  const [, setEditing] = useSearchParam('editPerson', asString);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const onSheet = new Set(people.map((p) => p.id));
  return (
    <>
      <table data-testid="people">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Claimed</th>
            <th />
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
                <td>
                  <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(p.id)}>
                    Edit
                  </button>
                </td>
                <td>
                  <ClaimList claims={own} />
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
    </>
  );
}

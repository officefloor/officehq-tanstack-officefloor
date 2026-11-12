import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from '../people/people';
import { claimsKey } from '../claims/claims';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { useFigure } from '../locale/locale';
import { useColumnShown } from '../columns/columns';

function StatementHead() {
  const shown = useColumnShown();
  if (!shown('statement-count') && !shown('stated-total') && !shown('statement-date')) return null;
  return <th>Statements</th>;
}

// A person's statements: how many were taken, the last one's due-total and date, a line for each still
// standing (oldest first, by date), and the date and button
// that take one (binding their counting claims to their figures now, or as they stood at the end of the
// date given; a blank date takes it now), and the button that cancels the last one
// (a steward only; its claims follow their live figures again). A refused action shows its error until
// the next action on the person is taken.
function PersonStatement({ person }: { person: Person }) {
  const fig = useFigure();
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [asOf, setAsOf] = useState('');
  const queryClient = useQueryClient();

  const take = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/statement`, { user, asOf: date }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
    },
  });

  const cancel = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/unstate`, { user }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]),
  });

  const latest = useLatestPersonAction(sheet, person.id);
  const shown = useColumnShown();

  if (!shown('statement-count') && !shown('stated-total') && !shown('statement-date')) return null;
  return (
    <td>
      {shown('statement-count') && <><span data-testid="person-statement-count">{person.statementCount}</span>{' '}</>}
      {shown('stated-total') && <><span data-testid="person-stated-total">{fig(person.statedTotal ?? '')}</span>{' '}</>}
      {shown('statement-date') && <><span data-testid="person-statement-date">{person.statementDate ?? ''}</span>{' '}</>}
      {person.statements.map((s, i) => (
        <div key={i} data-testid="person-statement-line-line">
          {s.date} {s.dueTotal}
        </div>
      ))}
      <input
        type="date"
        data-testid="person-statement-asof"
        value={asOf}
        onChange={(e) => {
          take.reset();
          setAsOf(e.target.value);
        }}
      />
      <button
        type="button"
        data-testid="person-statement"
        disabled={take.isPending}
        onClick={() => {
          take.reset();
          take.mutate(asOf);
          // The date goes with this statement only: a refused one's never carries into the next.
          setAsOf('');
        }}
      >
        Statement
      </button>{' '}
      <button
        type="button"
        data-testid="person-unstate"
        disabled={cancel.isPending}
        onClick={() => {
          cancel.reset();
          cancel.mutate();
        }}
      >
        Cancel statement
      </button>
      {take.isError && take.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {take.error instanceof HttpError && take.error.reason ? `${take.error.reason}.` : 'The statement was refused.'}
        </span>
      )}
      {cancel.isError && cancel.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {cancel.error instanceof HttpError && cancel.error.reason
            ? `${cancel.error.reason}.`
            : 'The cancelling was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 18, Component: StatementHead }),
  PersonColumns.fill({ order: 18, Component: PersonStatement }),
];

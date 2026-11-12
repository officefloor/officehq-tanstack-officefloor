import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { poolsKey } from '../pools/pools';

function HoldHead() {
  return <th>Hold</th>;
}

// Whether the person has any hold, a line for each of their holds, and the dates and button that add
// one (a blank from-date takes the sheet's today, a blank to-date has no end). Their OPEN claims dated
// within a hold count in no live total unless a statement bound them. A refused hold shows its error
// until the dates are changed or the next is tried.
function PersonHold({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [fields, setFields] = useState({ from: '', to: '' });
  const queryClient = useQueryClient();

  const hold = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (dates: { from: string; to: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/hold`, { ...dates, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      // a hold in force on today takes the person's cuts from their pools
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      setFields({ from: '', to: '' });
    },
  });

  const set = (k: 'from' | 'to') => (e: ChangeEvent<HTMLInputElement>) => {
    hold.reset();
    setFields((f) => ({ ...f, [k]: e.target.value }));
  };

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <span data-testid="person-hold">{person.held ? 'yes' : 'no'}</span>
      {person.holds.map((h, i) => (
        <div key={i} data-testid="person-hold-line">
          {h.from} – {h.to ?? 'open'}
        </div>
      ))}
      <input type="date" data-testid="person-hold-from" aria-label="Hold from" value={fields.from} onChange={set('from')} />
      <input type="date" data-testid="person-hold-to" aria-label="Hold to" value={fields.to} onChange={set('to')} />
      <button
        type="button"
        data-testid="person-hold-toggle"
        disabled={hold.isPending}
        onClick={() => {
          hold.reset();
          hold.mutate(fields);
        }}
      >
        Hold
      </button>
      {hold.isError && hold.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {hold.error instanceof HttpError && hold.error.reason ? `${hold.error.reason}.` : 'The hold was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.5, Component: HoldHead }),
  PersonColumns.fill({ order: 19.5, Component: PersonHold }),
];

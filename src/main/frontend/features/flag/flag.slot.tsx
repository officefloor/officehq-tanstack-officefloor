import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

function FlagHead() {
  return <th>Flag</th>;
}

// The reason of the person's flag while it is in force on the sheet's today (nothing otherwise), and the reason,
// until-date and button with which a steward flags them (a blank until-date takes the sheet's today). While the
// flag is in force only a steward may open a claim of theirs. A refused flag shows its error until the entries
// are changed or the next is tried.
function PersonFlag({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [fields, setFields] = useState({ reason: '', until: '' });
  const queryClient = useQueryClient();

  const flag = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (f: { reason: string; until: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/flag`, { ...f, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      setFields({ reason: '', until: '' });
    },
  });

  const set = (k: 'reason' | 'until') => (e: ChangeEvent<HTMLInputElement>) => {
    flag.reset();
    setFields((f) => ({ ...f, [k]: e.target.value }));
  };

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <span data-testid="person-flag-shown">{person.flag ?? ''}</span>
      <input data-testid="person-flag-reason" aria-label="Flag reason" placeholder="Reason" value={fields.reason} onChange={set('reason')} />
      <input type="date" data-testid="person-flag-until" aria-label="Flag until" value={fields.until} onChange={set('until')} />
      <button
        type="button"
        data-testid="person-flag"
        disabled={flag.isPending}
        onClick={() => {
          flag.reset();
          flag.mutate(fields);
        }}
      >
        Flag
      </button>
      {flag.isError && flag.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {flag.error instanceof HttpError && flag.error.reason ? `${flag.error.reason}.` : 'The flag was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.6, Component: FlagHead }),
  PersonColumns.fill({ order: 19.6, Component: PersonFlag }),
];

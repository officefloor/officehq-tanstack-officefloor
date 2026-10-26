import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { useUser } from '../session/useSession';

function FlagHeads(_: { sheetId: string; user: string }) {
  return (
    <>
      <th>Flag</th>
      <th />
    </>
  );
}

// The reason of the person's flag while the sheet's today is on or before its until date (blank otherwise), and the
// reason, until date and button that flag them; only a steward may. While the flag holds, only a steward may open the
// person's claims. A refusal shows on the person until the next action on their row.
function FlagCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const flag = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: (f: { reason: string; until: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/flag`, {
        reason: f.reason.trim(),
        until: f.until.trim(),
        by: user ?? '',
      }),
    onSuccess: async () => {
      setReason('');
      setUntil('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const last = useLastPersonAction(sheetId, personId, flag.submittedAt);

  return (
    <>
      <td data-testid="person-flag-shown">{person?.flagShown ?? ''}</td>
      <td>
        <input
          type="text"
          data-testid="person-flag-reason"
          value={reason}
          onChange={(e) => {
            setReason(e.target.value);
            flag.reset();
          }}
        />{' '}
        <input
          type="date"
          data-testid="person-flag-until"
          value={until}
          onChange={(e) => {
            setUntil(e.target.value);
            flag.reset();
          }}
        />{' '}
        <button type="button" data-testid="person-flag" disabled={flag.isPending} onClick={() => flag.mutate({ reason, until })}>
          Flag
        </button>
        {flag.isError && last && (
          <span data-testid="person-error" role="alert">
            {flag.error.message}
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 46, Component: FlagHeads }),
  PersonCells.fill({ order: 46, Component: FlagCells }),
];

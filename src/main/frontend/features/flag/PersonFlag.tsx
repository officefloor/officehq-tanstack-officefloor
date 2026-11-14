import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

// The reason of the person's flag while it is in force (the sheet's today on or before its until date; nothing
// after), and the reason, until date (blank: the sheet's today) and button a steward flags them with. While the flag
// is in force only a steward may open a claim of theirs.
export function PersonFlag({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const flag = useMutation({
    mutationFn: (body: { reason: string; until: string }) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/flag?user=${encodeURIComponent(user)}`,
        body,
      ),
    onSuccess: async () => {
      setReason('');
      setUntil('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
    onError: () => setError('The person could not be flagged: only a steward flags, with a reason.'),
  });

  const submit = () => {
    setError('');
    const r = reason.trim();
    const u = until.trim();
    if (!r) {
      setError('A flag needs a reason.');
      return;
    }
    if (u && !DATE.test(u)) {
      setError('Type a date as yyyy-mm-dd.');
      return;
    }
    once(() => flag.mutateAsync({ reason: r, until: u }));
  };

  return (
    <td>
      <span data-testid="person-flag-shown">{person.flag ?? ''}</span>{' '}
      <input
        data-testid="person-flag-reason"
        placeholder="Reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          setError('');
        }}
      />
      <input
        data-testid="person-flag-until"
        placeholder="Until yyyy-mm-dd"
        value={until}
        onChange={(e) => {
          setUntil(e.target.value);
          setError('');
        }}
      />
      <button type="button" data-testid="person-flag" disabled={flag.isPending} onClick={submit}>
        Flag
      </button>
    </td>
  );
}

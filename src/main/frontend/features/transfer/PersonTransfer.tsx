import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

// The entry of the sheet id to transfer the person to and the button that transfers them (a steward only; not a
// retired person, nor to this sheet). They arrive on the other sheet with their claims and leave this one, so
// everything on the sheet is re-read.
export function PersonTransfer({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [to, setTo] = useState('');
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const transfer = useMutation({
    mutationFn: (target: string) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/transfer?user=${encodeURIComponent(user)}`,
        { to: target },
      ),
    onSuccess: async () => {
      setTo('');
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
    onError: () => setError('The person could not be transferred.'),
  });

  const submit = () => {
    setError('');
    if (!to.trim()) {
      setError('Enter the sheet to transfer to.');
      return;
    }
    once(() => transfer.mutateAsync(to.trim()));
  };

  return (
    <td>
      <input
        data-testid="person-transfer-to"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      />
      <button type="button" data-testid="person-transfer" disabled={transfer.isPending} onClick={submit}>
        Transfer
      </button>
    </td>
  );
}

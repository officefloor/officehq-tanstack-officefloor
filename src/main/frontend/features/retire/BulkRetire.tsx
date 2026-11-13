import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { setPersonError } from '../people/personError';
import { useSelectedPeople } from '../people/selection';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

// Retires every ticked person from a date (blank: the sheet's today), one after another in list order.
// Each goes through the person's own retire, exactly as the row's retire button does, so it is audited
// and rolled up the same way. One the server refuses is left as it is and shows the refusal on its row;
// the count is of those retired.
export function BulkRetire() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const [selected, , clear] = useSelectedPeople();
  const [from, setFrom] = useState('');
  const [retired, setRetired] = useState<number | undefined>(undefined);
  const [busy, setBusy] = useState(false);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  if (!user || !sheet) return null;

  const run = async () => {
    setRetired(undefined);
    setBusy(true);
    const ordered = people.filter((p) => selected.has(p.id));
    const refused: number[] = [];
    let done = 0;
    for (const p of ordered) setPersonError(sheet, p.id, '');
    try {
      for (const p of ordered) {
        try {
          await postJson<Person>(
            `/api/sheets/${encodeURIComponent(sheet)}/people/${p.id}/retire?user=${encodeURIComponent(user)}`,
            { from: from.trim() },
          );
          done++;
        } catch {
          refused.push(p.id);
        }
      }
      clear();
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    } finally {
      setBusy(false);
      setRetired(done);
      for (const id of refused) setPersonError(sheet, id, 'That person could not be retired.');
    }
  };

  return (
    <p>
      <input
        data-testid="bulk-retire-date"
        type="date"
        title="Retire from (blank: the sheet's today)"
        value={from}
        onChange={(e) => setFrom(e.target.value)}
      />{' '}
      <button type="button" data-testid="bulk-retire" disabled={busy} onClick={() => once(run)}>
        Retire selected people
      </button>
      {retired !== undefined && (
        <>
          {' '}
          Retired <span data-testid="bulk-retired">{retired}</span>
        </>
      )}
    </p>
  );
}

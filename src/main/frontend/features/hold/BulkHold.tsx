import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { setPersonError } from '../people/personError';
import { useSelectedPeople } from '../people/selection';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

// Holds every ticked person from a date (blank: the sheet's today) to an optional to-date, one after another in
// list order. Each goes through the person's own hold, exactly as the row's hold button does, so it is audited and
// rolled up the same way. One the server refuses is left as it is and shows the refusal on its row; the count is
// of those held.
export function BulkHold() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const [selected, , clear] = useSelectedPeople();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [held, setHeld] = useState<number | undefined>(undefined);
  const [busy, setBusy] = useState(false);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  if (!user || !sheet) return null;

  const run = async () => {
    setHeld(undefined);
    setBusy(true);
    const ordered = people.filter((p) => selected.has(p.id));
    const refused: number[] = [];
    let done = 0;
    for (const p of ordered) setPersonError(sheet, p.id, '');
    const f = from.trim();
    const t = to.trim();
    try {
      for (const p of ordered) {
        try {
          await postJson<Person>(
            `/api/sheets/${encodeURIComponent(sheet)}/people/${p.id}/hold?user=${encodeURIComponent(user)}`,
            { from: f || null, to: t || null },
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
      setHeld(done);
      for (const id of refused) setPersonError(sheet, id, 'That person could not be held.');
    }
  };

  return (
    <p>
      <input
        data-testid="bulk-hold-from"
        placeholder="Hold from yyyy-mm-dd"
        value={from}
        onChange={(e) => setFrom(e.target.value)}
      />{' '}
      <input
        data-testid="bulk-hold-to"
        placeholder="To yyyy-mm-dd (blank: no end)"
        value={to}
        onChange={(e) => setTo(e.target.value)}
      />{' '}
      <button type="button" data-testid="bulk-hold" disabled={busy} onClick={() => once(run)}>
        Hold selected people
      </button>
      {held !== undefined && (
        <>
          {' '}
          Held <span data-testid="bulk-held">{held}</span>
        </>
      )}
    </p>
  );
}

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { clearRowError } from '../people/rowError';
import { useUser } from '../session/session';
import { clearSelectedPeople, useSelectedPeople } from '../bulkgrade/selection';

// Retires the ticked people together from a date ('' for the sheet's today). The server retires each
// in list order exactly as the row's retire would; the bar shows how many were retired. The count is
// cleared while it runs. A refusal shows the error and changes nothing.
function BulkRetire({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedPeople(sheetId);
  const [user] = useUser();
  const [from, setFrom] = useState('');
  const [retired, setRetired] = useState<number | null>(null);
  const [error, setError] = useState('');

  const once = useOnce();
  const apply = useMutation({
    mutationFn: (personIds: string[]) =>
      postJson<{ retired: number }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/bulk-retire`,
        { personIds, from },
        user,
      ),
    onSuccess: (r) => {
      setRetired(r.retired);
      clearSelectedPeople(sheetId);
      void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('The selected people could not be retired.'),
  });

  const press = () => {
    setRetired(null);
    setError('');
    const ids = [...selected];
    ids.forEach(clearRowError);
    once((done) => apply.mutate(ids, { onSettled: done }));
  };

  return (
    <section data-testid="bulk-retire-bar">
      <label>
        Retire from{' '}
        <input
          type="date"
          data-testid="bulk-retire-date"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="button" data-testid="bulk-retire" onClick={press} disabled={apply.isPending}>
        Retire selected ({selected.size})
      </button>
      {retired !== null && (
        <>
          {' '}Retired <span data-testid="bulk-retired">{retired}</span>
        </>
      )}
      {error && (
        <p role="alert" data-testid="person-error">
          {error}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.5, Component: BulkRetire });

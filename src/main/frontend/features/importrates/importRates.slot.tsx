import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';
import { ratesKey } from '../rates/useRates';
import { claimsKey } from '../claims/useClaims';

type ImportResult = { set: number };

// The rate import box: lines of grade,rate,from,to (from and to may be absent or blank), in order. The server checks
// every line with the rate form's rules first, so a refusal sets nothing; otherwise each line is set as the rate form
// sets it. Shows how many were set; the count is gone while it runs.
function ImportRates({ sheetId }: { sheetId: string; user: string }) {
  const [lines, setLines] = useState('');
  const [count, setCount] = useState<number | null>(null);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const importRates = useMutation({
    mutationFn: async (text: string) => {
      setCount(null);
      if (text.trim() === '') throw new Error('Paste at least one line of grade,rate,from,to');
      // `by` is the signed-in user the change is recorded against.
      const result = await postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheetId)}/rates/import`, {
        lines: text,
        by: user ?? '',
      });
      setCount(result.set);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      return result;
    },
  });

  const submit = useSubmitOnce(importRates);
  return (
    <form
      data-testid="import-rates-box"
      onSubmit={(e) => {
        e.preventDefault();
        submit(lines);
      }}
    >
      <label>
        Import rates (grade,rate,from,to per line)
        <br />
        <textarea
          data-testid="import-rates"
          rows={4}
          cols={50}
          value={lines}
          onChange={(e) => {
            setLines(e.target.value);
            importRates.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="import-rates-submit" disabled={importRates.isPending}>
        Import rates
      </button>{' '}
      {count !== null && !importRates.isError && (
        <span>
          Set <strong data-testid="import-rates-set">{count}</strong>
        </span>
      )}
      {importRates.isError && (
        <p data-testid="import-rates-error" role="alert">
          {importRates.error.message}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: 5.5, Component: ImportRates });

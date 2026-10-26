import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';
import { periodsKey } from '../periods/periods.slot';

type ImportResult = { added: number };

// The opening balance paste: lines of name,period,amount (a person on the sheet by name or alias, a period on the
// sheet, an amount of two decimals, 0.00 or more). The server checks every line first, so a refusal adds nothing;
// otherwise each line adds its amount to the person's carried figure for the period. Shows how many lines went
// through; the count is gone while it runs.
function ImportOpenings({ sheetId }: { sheetId: string; user: string }) {
  const [lines, setLines] = useState('');
  const [count, setCount] = useState<number | null>(null);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const importOpenings = useMutation({
    mutationFn: async (text: string) => {
      setCount(null);
      if (text.trim() === '') throw new Error('Paste at least one line of name,period,amount');
      // `by` is the signed-in user the change is recorded against.
      const result = await postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheetId)}/openings/import`, {
        lines: text,
        by: user ?? '',
      });
      setCount(result.added);
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      return result;
    },
  });

  const submit = useSubmitOnce(importOpenings);
  return (
    <form
      data-testid="opening-import-box"
      onSubmit={(e) => {
        e.preventDefault();
        submit(lines);
      }}
    >
      <label>
        Opening balances (name,period,amount per line)
        <br />
        <textarea
          data-testid="opening-import"
          rows={4}
          cols={50}
          value={lines}
          onChange={(e) => {
            setLines(e.target.value);
            importOpenings.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="opening-import-submit" disabled={importOpenings.isPending}>
        Paste openings
      </button>{' '}
      {count !== null && !importOpenings.isError && (
        <span>
          Added <strong data-testid="opening-import-done">{count}</strong>
        </span>
      )}
      {importOpenings.isError && (
        <p data-testid="opening-error" role="alert">
          {importOpenings.error.message}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: 7.5, Component: ImportOpenings });

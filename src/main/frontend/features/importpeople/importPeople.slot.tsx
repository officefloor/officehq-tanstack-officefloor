import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';
import { peopleKey } from '../people/people.slot';
import { claimsKey } from '../claims/useClaims';

type ImportResult = { added: number; updated: number };

// The import box: lines of name,grade,terms,contact (terms and contact may be absent or blank), in order. A line
// naming a person on the sheet (by name or alias) updates them as the person form edits them (blank terms or contact
// keep theirs); any other line adds a person as the form adds them. The server checks every line with the person
// form's rules first (a name or contact repeated in the import refuses it too), so a refusal imports nothing.
// Shows how many were added and updated; the counts are gone while it runs, and show before the new rows do.
function ImportPeople({ sheetId }: { sheetId: string; user: string }) {
  const [lines, setLines] = useState('');
  const [counts, setCounts] = useState<ImportResult | null>(null);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const importPeople = useMutation({
    mutationFn: async (text: string) => {
      setCounts(null);
      if (text.trim() === '') throw new Error('Paste at least one line of name,grade,terms,contact');
      // `by` is the signed-in user the change is recorded against.
      const result = await postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheetId)}/people/import`, {
        lines: text,
        by: user ?? '',
      });
      setCounts(result);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      return result;
    },
  });

  const submit = useSubmitOnce(importPeople);
  return (
    <form
      data-testid="import-box"
      onSubmit={(e) => {
        e.preventDefault();
        submit(lines);
      }}
    >
      <label>
        Import people (name,grade,terms,contact per line)
        <br />
        <textarea
          data-testid="import-people"
          rows={4}
          cols={50}
          value={lines}
          onChange={(e) => {
            setLines(e.target.value);
            importPeople.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="import-submit" disabled={importPeople.isPending}>
        Import people
      </button>{' '}
      {counts !== null && !importPeople.isError && (
        <span>
          Added <strong data-testid="import-added">{counts.added}</strong>, updated{' '}
          <strong data-testid="import-updated">{counts.updated}</strong>
        </span>
      )}
      {importPeople.isError && (
        <p data-testid="import-error" role="alert">
          {importPeople.error.message}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: 17.5, Component: ImportPeople });

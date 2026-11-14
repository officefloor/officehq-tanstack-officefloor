import { useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { sheetKey, useCurrentSheet, useCurrentUser } from './session';

// The date this screen previews the sheet on: every figure is shown as if the sheet's today were that date, over the
// sheet's current records. It belongs to this screen only (the `preview` search param, carried by every read in
// api/http); every change is still made by the sheet's real today, and nothing a change of today does happens.
export function Preview() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [preview] = useSearchParam('preview', asString);
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  if (!user || !sheet) return null;

  const show = async (value: string) => {
    await navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const next = { ...prev };
        if (value) next.preview = value;
        else delete next.preview;
        return next;
      },
      replace: true,
    });
    // Everything kept per sheet is re-read on the new date.
    await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = date.trim();
    setError('');
    if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed) || Number.isNaN(Date.parse(trimmed))) {
      setError('Type a date as yyyy-mm-dd.');
      return;
    }
    void show(trimmed);
  };

  return (
    <form onSubmit={submit}>
      <span>
        Preview: <strong data-testid="sheet-preview">{preview}</strong>{' '}
      </span>
      <input
        data-testid="preview-date"
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="preview-set">
        Set preview
      </button>
      <button
        type="button"
        data-testid="preview-clear"
        onClick={() => {
          setError('');
          void show('');
        }}
      >
        Clear preview
      </button>
      {error && <span data-testid="preview-error">{error}</span>}
    </form>
  );
}

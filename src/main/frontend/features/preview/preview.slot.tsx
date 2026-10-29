import { useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { setPreview, usePreview } from '../../api/preview';
import { SheetPanel } from '../../slots/defs/sheetPanel';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** Whether the text is a real date (yyyy-mm-dd). */
function isDate(text: string): boolean {
  if (!DATE.test(text)) return false;
  const d = new Date(`${text}T00:00:00Z`);
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === text;
}

/**
 * Previews the sheet at a date, on this user's screen only: every figure is shown as if the sheet's today were that
 * date, over the sheet's current records. Every change still uses the sheet's real today, and nothing a change of
 * today does (snapshots, scores, auto-closing) happens. Clearing it shows the sheet at its real today.
 */
function PreviewDate() {
  const preview = usePreview();
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState('');
  const [error, setError] = useState('');

  // Every query kept per sheet is read afresh for the new date; none shows what it read for another.
  const show = (date: string) => {
    setPreview(date);
    void queryClient.resetQueries({ queryKey: ['sheet'] });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const date = typed.trim();
    if (!isDate(date)) {
      setError('The preview date must be a date (yyyy-mm-dd).');
      return;
    }
    setError('');
    show(date);
  };

  return (
    <form data-testid="preview-form" onSubmit={submit}>
      Preview at <strong data-testid="sheet-preview">{preview}</strong>{' '}
      <input
        data-testid="preview-date"
        placeholder="yyyy-mm-dd"
        value={typed}
        onChange={(e) => {
          setTyped(e.target.value);
          setError('');
        }}
      />{' '}
      <button type="submit" data-testid="preview-set">
        Preview
      </button>{' '}
      <button
        type="button"
        data-testid="preview-clear"
        onClick={() => {
          setError('');
          setTyped('');
          show('');
        }}
      >
        Clear preview
      </button>
      {error && <p data-testid="preview-error">{error}</p>}
    </form>
  );
}

export const contribution = SheetPanel.fill({ order: 0.55, Component: PreviewDate });

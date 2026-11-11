import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { PREVIEW_PARAM } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** The preview date the signed-in user's screen shows the sheet at ('' for none). */
export const usePreview = () => useSearchParam(PREVIEW_PARAM, asString);

// The preview date: an entry, a set and a clear button, and the preview date shown (nothing while
// unset). While it is set every figure on the screen is shown as if the sheet's today were that date,
// over the sheet's current records (the server answers each read so); every change still uses the
// sheet's real today, and nothing a change of today does happens. It lives in this user's URL, so
// other users' screens are unaffected. Setting or clearing it re-reads everything.
function PreviewDate() {
  const queryClient = useQueryClient();
  const [preview, setPreview] = usePreview();
  const [input, setInput] = useState('');
  const [error, setError] = useState('');

  const shown = useRef(preview);
  useEffect(() => {
    if (shown.current === preview) return;
    shown.current = preview;
    void queryClient.resetQueries();
  }, [preview, queryClient]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const date = input.trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      setError('Choose a date to preview the sheet at.');
      return;
    }
    setPreview(date);
  };

  return (
    <form onSubmit={submit} data-testid="preview-form">
      Preview: <strong data-testid="sheet-preview">{preview}</strong>{' '}
      <label>
        Preview at{' '}
        <input
          type="date"
          data-testid="preview-date"
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="preview-set">
        Set
      </button>{' '}
      <button
        type="button"
        data-testid="preview-clear"
        onClick={() => {
          setError('');
          setPreview(undefined);
        }}
      >
        Clear
      </button>
      {error && (
        <p role="alert" data-testid="preview-error">
          {error}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: -98, Component: PreviewDate });

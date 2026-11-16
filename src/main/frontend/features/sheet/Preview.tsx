import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useUser } from '../session/session';

/** The date this user's screen previews as the sheet's today ('': none). It lives in the URL (`preview`,
 *  with `preview-by` naming whose it is), so it belongs to this screen and its signed-in user alone, and
 *  every read about the sheet carries it (api/http). */
export function usePreview(): [string, (value: string) => Promise<void>] {
  const [user] = useUser();
  const [preview, setPreview] = useSearchParam('preview', asString);
  const [by, setBy] = useSearchParam('preview-by', asString);
  const set = async (value: string) => {
    await setBy(value === '' ? undefined : user);
    await setPreview(value);
  };
  return [by === user ? preview : '', set];
}

/** Set or clear the preview date. While it is set every figure on the screen is shown as if the sheet's
 *  today were that date, over the sheet's current records; every change still goes by its real today. */
export function Preview() {
  const queryClient = useQueryClient();
  const [preview, setPreview] = usePreview();
  const [date, setDate] = useState('');

  // Everything on screen is re-read once the URL carries the new date.
  const show = async (value: string) => {
    await setPreview(value);
    await queryClient.resetQueries();
  };

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const value = date.trim();
        if (value !== '') void show(value);
      }}
    >
      Preview <span data-testid="sheet-preview">{preview}</span>{' '}
      <input
        type="date"
        data-testid="preview-date"
        value={date}
        onChange={(e) => setDate(e.target.value)}
      />{' '}
      <button type="submit" data-testid="preview-set">
        Preview date
      </button>{' '}
      <button type="button" data-testid="preview-clear" onClick={() => void show('')}>
        Clear preview
      </button>
    </form>
  );
}

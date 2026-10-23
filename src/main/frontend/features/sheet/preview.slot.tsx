import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { setRequestPreview } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import { usePreview } from './sheet';

/** Whether the text is a real date as yyyy-mm-dd. */
function isDate(text: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return false;
  const d = new Date(`${text}T00:00:00Z`);
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === text;
}

/** The preview date of this user's screen: shown ('' when none), set and cleared. Setting or clearing it re-reads
 *  everything under the sheet, its current records figured as if its today were that date, or on its real today.
 *  Changes are made on the real today all the while, and nothing a change of today does happens. */
function SheetPreview({ sheetId }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [preview, setPreview] = usePreview();
  const [typed, setTyped] = useState('');
  const [invalid, setInvalid] = useState(false);

  useEffect(() => {
    setRequestPreview(preview);
  }, [preview]);

  const show = async (date: string) => {
    setRequestPreview(date);
    setPreview(date === '' ? undefined : date);
    await queryClient.resetQueries({ queryKey: ['sheet', sheetId] });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const date = typed.trim();
    if (!isDate(date)) {
      setInvalid(true);
      return;
    }
    setInvalid(false);
    void show(date);
  };

  return (
    <section>
      <form data-testid="preview-form" onSubmit={submit}>
        <label>
          Preview today as{' '}
          <input
            data-testid="preview-date"
            placeholder="yyyy-mm-dd"
            value={typed}
            onChange={(e) => {
              setTyped(e.target.value);
              setInvalid(false);
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="preview-set">
          Preview
        </button>{' '}
        <button
          type="button"
          data-testid="preview-clear"
          onClick={() => {
            setInvalid(false);
            void show('');
          }}
        >
          Clear preview
        </button>
      </form>
      <p>
        Previewing today as <strong data-testid="sheet-preview">{preview}</strong>
      </p>
      {invalid && (
        <p data-testid="preview-error" role="alert">
          Enter the preview date as a real date (yyyy-mm-dd).
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 0.6, Component: SheetPreview });

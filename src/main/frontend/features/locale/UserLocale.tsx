import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { showFigures } from './figures';
import { LOCALES, localeKey, useLocale } from './locale';
import type { Locale } from './locale';

// The signed-in user's locale: a select and its save, and the locale saved. Under de every figure the
// screen shows is 1.234,56; under en as the app renders it. Entries are still typed with a dot.
export function UserLocale() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const saved = useLocale().data?.locale;
  const [chosen, setChosen] = useState<Locale>('en');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  useEffect(() => {
    if (saved) setChosen(saved);
  }, [saved]);

  const locale: Locale = user && sheet && saved ? saved : 'en';
  useEffect(() => showFigures(document.body, locale), [locale]);

  const save = useMutation({
    mutationFn: (value: Locale) =>
      postJson<{ user: string; locale: Locale }>(
        `/api/sheets/${encodeURIComponent(sheet)}/locale?user=${encodeURIComponent(user)}`,
        { locale: value },
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: localeKey(sheet, user) });
    },
    onError: () => setError('The locale could not be saved.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => save.mutateAsync(chosen));
  };

  return (
    <form onSubmit={submit}>
      <span>
        Locale: <strong data-testid="user-locale-current">{saved ?? ''}</strong>{' '}
      </span>
      <select
        data-testid="user-locale"
        value={chosen}
        onChange={(e) => {
          setChosen(e.target.value as Locale);
          setError('');
        }}
      >
        {LOCALES.map((l) => (
          <option key={l} value={l}>
            {l}
          </option>
        ))}
      </select>
      <button type="submit" data-testid="user-locale-save" disabled={save.isPending}>
        Save locale
      </button>
      {error && <span data-testid="user-locale-error">{error}</span>}
    </form>
  );
}

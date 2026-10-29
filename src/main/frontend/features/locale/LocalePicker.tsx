import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { LOCALES, localeKey, localeQuery } from './localeQuery';
import type { Locale } from './localeQuery';

/** The signed-in user's locale, and the select that changes it. Kept per user; nothing is written to the audit file. */
export function LocalePicker({ user }: { user: string }) {
  const queryClient = useQueryClient();
  const current = useQuery(localeQuery(user)).data ?? 'en';
  const [chosen, setChosen] = useState<Locale>(current);
  const [error, setError] = useState('');
  useEffect(() => setChosen(current), [current]);
  const save = useMutation({
    mutationFn: (locale: Locale) =>
      putJson<{ locale: Locale }>(`/api/users/${encodeURIComponent(user)}/locale`, { locale }, user),
    onSuccess: (r) => queryClient.setQueryData(localeKey(user), r.locale),
    onError: () => setError('The locale could not be saved.'),
  });

  return (
    <p>
      Locale <strong data-testid="user-locale-current">{current}</strong>{' '}
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
      </select>{' '}
      <button
        type="button"
        data-testid="user-locale-save"
        disabled={save.isPending}
        onClick={() => {
          setError('');
          save.mutate(chosen);
        }}
      >
        Save locale
      </button>
      {error && <span data-testid="user-locale-error">{error}</span>}
    </p>
  );
}

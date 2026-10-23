import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { LOCALES, localeKey, localePath, useLocale } from './locale';
import type { Locale, UserLocale as Saved } from './locale';

/** The signed-in user's locale: shown, and chosen with a select (saved per user; no audit line). */
export function UserLocale({ user }: { user: string }) {
  const queryClient = useQueryClient();
  const current = useLocale(user);
  const [chosen, setChosen] = useState<Locale>(current);
  const once = useSubmitOnce();

  useEffect(() => setChosen(current), [current]);

  const save = useMutation({
    mutationFn: (locale: Locale) => putJson<Saved>(localePath(user), { locale }),
    onSuccess: (saved) => queryClient.setQueryData(localeKey(user), saved),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.reset();
    if (!once.start()) return;
    save.mutate(chosen);
  };

  return (
    <form data-testid="user-locale-form" onSubmit={submit}>
      Locale <strong data-testid="user-locale-current">{current}</strong>{' '}
      <label>
        Show figures as{' '}
        <select
          data-testid="user-locale"
          value={chosen}
          onChange={(e) => {
            setChosen(e.target.value as Locale);
            save.reset();
          }}
        >
          {LOCALES.map((l) => (
            <option key={l} value={l}>
              {l}
            </option>
          ))}
        </select>
      </label>{' '}
      <button type="submit" data-testid="user-locale-save" disabled={save.isPending}>
        Save locale
      </button>
      {save.isError && (
        <p data-testid="user-locale-error" role="alert">
          {save.error instanceof HttpError && save.error.body ? save.error.body : 'Could not change the locale.'}
        </p>
      )}
    </form>
  );
}

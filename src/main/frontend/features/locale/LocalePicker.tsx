import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { LOCALES, localeKey, localePath, useLocale, type UserLocale } from './useLocale';

/** The signed-in user's locale: how the screen shows them figures. Saved per user; no audit line. */
export function LocalePicker({ user }: { user: string }) {
  const queryClient = useQueryClient();
  const locale = useLocale(user);
  // The locale picked but not yet saved; null while it is the saved one.
  const [picked, setPicked] = useState<string | null>(null);

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (chosen: string) => postJson<UserLocale>(localePath(user), { locale: chosen }),
    onSuccess: (saved) => {
      queryClient.setQueryData(localeKey(user), saved);
      setPicked(null);
    },
  });

  return (
    <form
      data-testid="user-locale-form"
      onSubmit={(e) => {
        e.preventDefault();
        save.reset();
        submitOnce(() => save.mutateAsync(picked ?? locale));
      }}
    >
      <label>
        Locale{' '}
        <select
          data-testid="user-locale"
          value={picked ?? locale}
          onChange={(e) => {
            save.reset();
            setPicked(e.target.value);
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
        Save
      </button>{' '}
      (now <span data-testid="user-locale-current">{locale}</span>)
      {save.isError && (
        <span data-testid="user-locale-error" role="alert">
          {' '}
          Could not save the locale.
        </span>
      )}
    </form>
  );
}

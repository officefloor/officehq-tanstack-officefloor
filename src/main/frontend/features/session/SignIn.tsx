import { useState } from 'react';
import { useCurrentUser } from './useSession';

/** Sign in by typing a name; there is no password. */
export function SignIn() {
  const [, setUser] = useCurrentUser();
  const [name, setName] = useState('');

  return (
    <form
      data-testid="signin-form"
      onSubmit={(e) => {
        e.preventDefault();
        const trimmed = name.trim();
        if (trimmed !== '') setUser(trimmed);
      }}
    >
      <label>
        Name{' '}
        <input data-testid="signin-name" value={name} onChange={(e) => setName(e.target.value)} />
      </label>{' '}
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
    </form>
  );
}

export function CurrentUser({ user }: { user: string }) {
  return (
    <p>
      Signed in as <strong data-testid="current-user">{user}</strong>
    </p>
  );
}

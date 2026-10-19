import { useState } from 'react';
import { asString, useSearchParam } from '../../url/useSearchParam';

// Sign in by name (no password). The signed-in name is URL state (`user`), so it survives a reload.
export function useCurrentUser(): [string, (user: string | undefined) => void] {
  return useSearchParam('user', asString);
}

export function SignIn() {
  const [user, setUser] = useCurrentUser();
  const [name, setName] = useState('');

  if (user) {
    return (
      <p>
        Signed in as <strong data-testid="current-user">{user}</strong>
      </p>
    );
  }
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const trimmed = name.trim();
        if (trimmed) setUser(trimmed);
      }}
    >
      <label>
        Name <input data-testid="signin-name" value={name} onChange={(e) => setName(e.target.value)} />
      </label>
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
    </form>
  );
}

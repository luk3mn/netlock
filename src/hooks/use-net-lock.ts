import { useCallback, useEffect, useState } from 'react';
import NetLock, { type Status } from '../../modules/net-lock';

export function useNetLock() {
  const [status, setStatus] = useState<Status>(() => NetLock.status());

  const refresh = useCallback(() => setStatus(NetLock.status()), []);

  useEffect(() => {
    const id = setInterval(refresh, 1000);
    return () => clearInterval(id);
  }, [refresh]);

  return { status, refresh };
}
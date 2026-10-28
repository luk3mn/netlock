import { requireNativeModule } from 'expo-modules-core';

export type Status = {
  isOwner: boolean;
  active: boolean;
  host: string;
  unlockAt: number;
  canDisable: boolean;
};

type NetLockModule = {
  status(): Status;
  configure(host: string): void;
  enable(): void;
  requestDisable(): number;
  cancelDisable(): void;
  disable(): void;
  removeOwner(): void;
};

export default requireNativeModule<NetLockModule>('NetLock');
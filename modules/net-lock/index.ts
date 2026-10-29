import { requireNativeModule } from 'expo-modules-core';

export type Status = {
  secureSettings: boolean;
  admin: boolean;
  accessibility: boolean;
  ready: boolean;
  active: boolean;
  host: string;
  requested: boolean;
  waitMs: number;
  canDisable: boolean;
};

type NetLockModule = {
  status(): Status;
  configure(host: string): void;
  enable(): void;
  requestDisable(): void;
  cancelDisable(): void;
  disable(): void;
  requestAdmin(): void;
  openAccessibilitySettings(): void;
};

export default requireNativeModule<NetLockModule>('NetLock');
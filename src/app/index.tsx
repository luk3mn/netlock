import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { router } from 'expo-router';
import { Alert } from 'react-native';
import NetLock from '../../modules/net-lock';
import { Button } from '../components/button';
import { useNetLock } from '../hooks/use-net-lock';

const formatRemaining = (ms: number) => {
  const total = Math.max(0, Math.floor(ms / 1000));
  const h = String(Math.floor(total / 3600)).padStart(2, '0');
  const m = String(Math.floor((total % 3600) / 60)).padStart(2, '0');
  const s = String(total % 60).padStart(2, '0');
  return `${h}:${m}:${s}`;
};

export default function Home() {
  const { status, now, refresh } = useNetLock();
  const waiting = status.active && status.unlockAt > now;
  const requested = status.active && status.unlockAt !== 0;

  const run = (action: () => unknown) => {
    try {
      action();
    } catch (error) {
      Alert.alert('Erro', error instanceof Error ? error.message : String(error));
    }
    refresh();
  };

  const confirmRequest = () =>
    Alert.alert(
      'Pedir desativação?',
      'A proteção só poderá ser desligada após o período de espera, em uma janela de 30 minutos.',
      [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Pedir', style: 'destructive', onPress: () => run(() => NetLock.requestDisable()) },
      ]
    );

  return (
    <ThemedView style={{ flex: 1, justifyContent: 'center', padding: 16 }}>
      <ThemedText style={{ textAlign: 'center', fontSize: 20, fontWeight: 'bold', marginBottom: 16 }}>
        {status.active ? 'Proteção ativa' : 'Proteção desligada'}
      </ThemedText>

      {!status.isOwner && (
        <ThemedText style={{ textAlign: 'center', fontSize: 16, color: '#f87171' }}>
          O app ainda não é Device Owner. Rode o comando adb.
        </ThemedText>
      )}

      {waiting && (
        <ThemedText style={{ textAlign: 'center', fontSize: 16, color: '#a3a3a3' }}>
          Liberação em {formatRemaining(status.unlockAt - now)}
        </ThemedText>
      )}

      {status.canDisable && (
        <ThemedText style={{ textAlign: 'center', fontSize: 16, color: '#10b981' }}>
          Liberado. Você pode desativar agora.
        </ThemedText>
      )}

      {!status.active && (
        <Button
          label="Ativar proteção"
          disabled={!status.isOwner || !status.host}
          onPress={() => run(() => NetLock.enable())}
        />
      )}

      {status.active && !requested && (
        <Button label="Pedir desativação" tone="danger" onPress={confirmRequest} />
      )}

      {waiting && (
        <Button label="Cancelar pedido" tone="muted" onPress={() => run(() => NetLock.cancelDisable())} />
      )}

      {status.canDisable && (
        <Button label="Desativar agora" tone="danger" onPress={() => run(() => NetLock.disable())} />
      )}

      <Button label="Configurações" tone="muted" onPress={() => router.push('/config')} />
    </ThemedView>
  );
}
import { router } from 'expo-router';
import { Alert, Text, View } from 'react-native';
import NetLock from '../../modules/net-lock';
import { Button } from '../components/button';
import { useNetLock } from '../hooks/use-net-lock';

const formatRemaining = (ms: number) => {
  const total = Math.max(0, Math.ceil(ms / 1000));
  const h = String(Math.floor(total / 3600)).padStart(2, '0');
  const m = String(Math.floor((total % 3600) / 60)).padStart(2, '0');
  const s = String(total % 60).padStart(2, '0');
  return `${h}:${m}:${s}`;
};

export default function Home() {
  const { status, refresh } = useNetLock();
  const waiting = status.active && status.requested && !status.canDisable;

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
      'A proteção só poderá ser desligada após o período de espera, em uma janela de 30 minutos. Reiniciar o aparelho cancela o pedido.',
      [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Pedir', style: 'destructive', onPress: () => run(() => NetLock.requestDisable()) },
      ]
    );

  return (
    <View style={{ flex: 1, justifyContent: 'center', padding: 20, backgroundColor: '#18181b' }}>
      <Text style={{ textAlign: 'center', fontSize: 24, fontWeight: 'bold', color: '#ffffff' }}>
        {status.active ? 'Proteção ativa' : 'Proteção desligada'}
      </Text>

      {!status.active && !status.isOwner && (
        <Text style={{ textAlign: 'center', color: '#f59e0b', marginTop: 10 }}>
          O app ainda não é Device Owner. Rode o comando adb.
        </Text>
      )}

      {waiting && (
        <Text style={{ textAlign: 'center', color: '#f59e0b', marginTop: 10 }}>
          Liberação em {formatRemaining(status.waitMs)}
        </Text>
      )}

      {status.canDisable && (
        <Text style={{ textAlign: 'center', color: '#10b981', marginTop: 10 }}>
          Liberado. Você pode desativar agora.
        </Text>
      )}

      {!status.active && (
        <Button
          label="Ativar proteção"
          disabled={!status.isOwner || !status.host}
          onPress={() => run(() => NetLock.enable())}
        />
      )}

      {status.active && !status.requested && (
        <Button label="Pedir desativação" tone="danger" onPress={confirmRequest} />
      )}

      {waiting && (
        <Button label="Cancelar pedido" tone="muted" onPress={() => run(() => NetLock.cancelDisable())} />
      )}

      {status.canDisable && (
        <Button label="Desativar agora" tone="danger" onPress={() => run(() => NetLock.disable())} />
      )}

      <Button label="Configurações" tone="muted" onPress={() => router.push('/config')} />
    </View>
  );
}
import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, Text, TextInput, View } from 'react-native';
import NetLock from '../../modules/net-lock';
import { Button } from '../components/button';
import { useNetLock } from '../hooks/use-net-lock';

export default function Config() {
  const { status, refresh } = useNetLock();
  const [host, setHost] = useState(status.host);

  const save = () => {
    try {
      NetLock.configure(host);
      refresh();
      router.back();
    } catch (error) {
      Alert.alert('Erro', error instanceof Error ? error.message : String(error));
    }
  };

  return (
    <View className="flex-1 gap-4 p-6">
      <Text className="text-white">Host do Private DNS (NextDNS)</Text>
      <TextInput
        value={host}
        onChangeText={setHost}
        editable={!status.active}
        autoCapitalize="none"
        autoCorrect={false}
        placeholder="abc123.dns.nextdns.io"
        placeholderTextColor="#71717a"
        className="rounded-2xl bg-zinc-900 p-4 text-white"
      />
      {status.active && (
        <Text className="text-amber-400">Bloqueado enquanto a proteção estiver ativa.</Text>
      )}
      <Button label="Salvar" disabled={status.active || !host.trim()} onPress={save} />
    </View>
  );
}
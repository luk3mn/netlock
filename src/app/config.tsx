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
    <View style={{ flex: 1, padding: 16, backgroundColor: '#1f2937' }}>
      <Text style={{ color: '#fff', fontWeight: 'bold' }}>Host do Private DNS (NextDNS)</Text>
      <TextInput
        value={host}
        onChangeText={setHost}
        editable={!status.active}
        autoCapitalize="none"
        autoCorrect={false}
        placeholder="abc123.dns.nextdns.io"
        placeholderTextColor="#71717a"
        style={{
          color: '#fff',
          borderColor: '#71717a',
          borderWidth: 1,
          borderRadius: 4,
          padding: 8,
        }}
      />
      {status.active && (
        <Text style={{ color: '#f59e0b', fontWeight: 'bold' }}>Bloqueado enquanto a proteção estiver ativa.</Text>
      )}
      <Button label="Salvar" disabled={status.active || !host.trim()} onPress={save} />
    </View>
  );
}
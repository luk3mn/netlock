import { DarkTheme, DefaultTheme, SplashScreen, Stack, ThemeProvider } from 'expo-router';
import '../global.css';

import { AnimatedSplashOverlay } from '@/components/animated-icon';
import { useColorScheme } from 'react-native';

SplashScreen.preventAutoHideAsync();

export default function Layout() {
  const colorScheme = useColorScheme();

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <AnimatedSplashOverlay />
      <Stack
        screenOptions={{
          headerStyle: { backgroundColor: '#09090b' },
          headerTintColor: '#fff',
          contentStyle: { backgroundColor: '#09090b' },
        }}
      >
        <Stack.Screen name="index" options={{ title: 'NetLock' }} />
        <Stack.Screen name="config" options={{ title: 'Configurações' }} />
      </Stack>
    </ThemeProvider>
  );
}
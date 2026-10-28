import { DarkTheme, DefaultTheme, SplashScreen, Stack, ThemeProvider } from 'expo-router';
import '../global.css';

import { AnimatedSplashOverlay } from '@/components/animated-icon';
import { StatusBar, useColorScheme } from 'react-native';

SplashScreen.preventAutoHideAsync();

export default function Layout() {
  const colorScheme = useColorScheme();

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <AnimatedSplashOverlay />
      <StatusBar barStyle={colorScheme === 'dark' ? 'light-content' : 'dark-content'} />
      <Stack>
        <Stack.Screen name="index" options={{ title: 'NetLock' }} />
        <Stack.Screen name="config" options={{ title: 'Configurações' }} />
      </Stack>
    </ThemeProvider>
  );
}
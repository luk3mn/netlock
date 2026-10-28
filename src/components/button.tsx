import { Pressable, Text } from 'react-native';

const tones = {
  primary: 'bg-emerald-500',
  danger: 'bg-red-500',
  muted: 'bg-zinc-800',
};

type ButtonProps = {
  label: string;
  onPress: () => void;
  tone?: keyof typeof tones;
  disabled?: boolean;
};

export function Button({ label, onPress, tone = 'primary', disabled }: ButtonProps) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={{ opacity: disabled ? 0.5 : 1, paddingVertical: 12, paddingHorizontal: 24, borderRadius: 8, backgroundColor: tones[tone], marginTop: 10 }}
    >
      <Text style={{ textAlign: 'center', fontWeight: 'bold', color: '#fff' }}>{label}</Text>
    </Pressable>
  );
}
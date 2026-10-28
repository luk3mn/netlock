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
      className={`rounded-2xl p-4 active:opacity-80 ${tones[tone]} ${disabled ? 'opacity-40' : ''}`}
    >
      <Text className="text-center font-semibold text-white">{label}</Text>
    </Pressable>
  );
}
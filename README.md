# NetLock

App Android (Expo + módulo nativo em Kotlin) que fixa um **Private DNS** (como o do NextDNS) no aparelho e dificulta desligá-lo. Para desativar, é preciso pedir a desativação dentro do app e **esperar um tempo configurável**.

O projeto é de uso pessoal e instalado por APK, fora da Play Store.

## Como funciona

| Peça | Função |
|---|---|
| `WRITE_SECURE_SETTINGS` | Permite ao app escrever o Private DNS do sistema. Concedida uma vez via `adb` |
| Serviço de acessibilidade | Refaz o DNS na hora se alguém alterar e fecha as telas de Configurações que permitiriam desinstalar, forçar parada ou desligar o próprio serviço |
| Device Admin | Bloqueia a desinstalação normal enquanto estiver ativo |
| Delay de desativação | Para desligar, é preciso pedir e esperar `DELAY_MS`. Depois abre uma janela de `WINDOW_MS` para confirmar |

O contador usa `elapsedRealtime`, então mudar a data do aparelho não adianta o tempo. Reiniciar o aparelho **cancela** o pedido em andamento.

O app **não** é Device Owner. Por isso existe sempre uma saída sem formatar o aparelho (veja [Saídas de emergência](#saídas-de-emergência)).

## Requisitos

- Android 9 ou superior (Private DNS) e depuração USB habilitada
- Node.js, JDK 17 e Android SDK, com `adb` no PATH
- Um perfil no [NextDNS](https://nextdns.io) e o host da aba **Setup > Private DNS**, no formato `<id>.dns.nextdns.io`

## Instalação

```bash
git clone <url-do-repositorio>
cd netlock
npm install
```

Confirme que o pacote em `app.json` é o usado nos comandos abaixo:

```json
"android": {
  "package": "com.luk3mn.netlock"
}
```

Conecte o celular por USB e instale a build release:

```bash
npx expo run:android --variant release --device
```

Alternativa gerando o APK manualmente:

```bash
npx expo prebuild --platform android
cd android
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

A pasta `android/` é gerada pelo `prebuild` e não vai para o git. Todo o código nativo fica em `modules/net-lock/`.

## Configuração inicial

Faça nesta ordem.

### 1. Permissão de configurações seguras

```bash
adb shell pm grant com.luk3mn.netlock android.permission.WRITE_SECURE_SETTINGS
```

Confirme:

```bash
adb shell dumpsys package com.luk3mn.netlock | grep WRITE_SECURE_SETTINGS
```

A saída deve conter `granted=true`.

No Samsung com **Auto Blocker**, desative antes "Bloquear comandos por cabo USB" em Configurações > Segurança e privacidade > Auto Blocker.

### 2. Administrador do dispositivo

No app, toque em **Conceder** ao lado de "Administrador do dispositivo" e confirme.

Se o botão não abrir a tela do sistema, ative manualmente em Configurações > Segurança e privacidade > Mais configurações de segurança > Apps administradores do dispositivo > netlock.

### 3. Serviço de acessibilidade

No app, toque em **Abrir** ao lado de "Serviço de acessibilidade". Em **Aplicativos instalados**, toque em **netlock**, ligue o toggle e confirme o aviso.

Se o toggle estiver bloqueado, abra Configurações > Apps > netlock > ⋮ > **Permitir configurações restritas** e tente de novo.

### 4. Bateria

Configurações > Apps > netlock > Bateria > **Sem restrições**. Confirme também que o app não está em "Apps em suspensão" em Configurações > Bateria > Limites de uso em segundo plano.

Na mesma tela de informações do app, mantenha **desligado** o toggle "Gerenciar app se não utilizado". Ele pode revogar permissões de apps parados.

### 5. Host do DNS e ativação

1. No app, abra **Configurações**, cole o host `<id>.dns.nextdns.io` e salve.
2. Volte à tela principal. Os três itens da checklist devem estar com ✓.
3. Toque em **Ativar proteção**.

Para validar, abra [test.nextdns.io](https://test.nextdns.io). Ele deve mostrar o seu perfil e o protocolo DoT.

## Uso diário

Com a proteção ativa:

- Alterar o DNS privado nas Configurações é revertido automaticamente.
- Abrir a Info do app, tentar desinstalar ou forçar a parada fecha a tela e volta à home.
- O host só pode ser alterado com a proteção desligada.

Para desativar:

1. Toque em **Pedir desativação** e confirme.
2. Aguarde o contador chegar a zero. Não reinicie o aparelho, senão o pedido é cancelado.
3. Quando aparecer "Liberado", toque em **Desativar agora** dentro da janela.

**Cancelar pedido** interrompe a espera a qualquer momento.

## Ajustar o tempo de espera

Em `modules/net-lock/android/src/main/java/expo/modules/netlock/NetLockPolicy.kt`:

```kotlin
const val DELAY_MS = 24L * 60 * 60 * 1000
const val WINDOW_MS = 2L * 60 * 60 * 1000
```

- `DELAY_MS`: espera entre pedir e poder desativar.
- `WINDOW_MS`: tempo que o botão "Desativar agora" fica disponível. Se passar, o pedido expira.

Atualize também o texto do alerta em `src/app/index.tsx`, que cita a janela. Com esperas longas, mantenha a janela generosa.

## Atualizar o app

Instale a nova versão **com a proteção desligada**:

```bash
npx expo run:android --variant release --device
```

A reinstalação por cima mantém o `WRITE_SECURE_SETTINGS`. Instale sempre pelo build local, para manter a mesma assinatura. Um APK assinado por outra chave não substitui a versão instalada, e com a proteção ativa você não consegue desinstalar a antiga.

## Saídas de emergência

Pelo PC, com depuração USB ligada:

```bash
adb uninstall com.luk3mn.netlock
```

Para apenas devolver o DNS ao modo automático:

```bash
adb shell settings put global private_dns_mode opportunistic
```

Pelo próprio celular, reinicie em **modo seguro** (segure o botão de desligar, toque e segure "Desligar" e confirme). Lá o serviço de acessibilidade não roda. Desative o admin em Configurações > Segurança e privacidade > Mais configurações de segurança > Apps administradores do dispositivo e desinstale o app.

Essas saídas existem de propósito, para que um bug não prenda o aparelho. O delay protege contra o impulso, não contra quem procura um atalho.

## Solução de problemas

| Sintoma | Causa e correção |
|---|---|
| `Failure [package not found]` no `pm grant` | Pacote diferente do esperado. Veja com `adb shell pm list packages \| grep -i netlock` |
| `SecurityException` no `pm grant` | Auto Blocker da One UI ativo. Desative o bloqueio de comandos por USB |
| Botão **Conceder** não responde | A tela de Device Admin não abre com `FLAG_ACTIVITY_NEW_TASK`. A função `requestAdmin` deve usar `appContext.currentActivity`. Ative manualmente pelo caminho da etapa 2 |
| Toggle da acessibilidade bloqueado | Use "Permitir configurações restritas" na Info do app |
| DNS não volta sozinho ao ser alterado | O serviço de acessibilidade foi encerrado. Revise a bateria (etapa 4) e religue o serviço |
| Uma tela sensível não fecha | Os textos variam por idioma e versão da One UI. Ajuste a lista `sensitive` em `NetLockAccessibilityService.kt` |
| Sem internet após ativar | O Private DNS em modo `hostname` não tem fallback. Verifique o host, o perfil e o limite de consultas do plano no NextDNS |
| `Native module not found` ou `undefined is not a function` | O app instalado foi compilado antes de o código nativo mudar. Recompile e reinstale |

## Limitações

- Quem tem acesso ao `adb` ou ao modo seguro pode desfazer tudo. É o preço de não usar Device Owner.
- O filtro é por DNS. Navegadores ou apps com DNS próprio (DoH customizado) ignoram o Private DNS do sistema.
- O Private DNS depende de o host do NextDNS responder. Se ele parar, o aparelho fica sem resolver domínios até você desativar a proteção.
- Reiniciar o aparelho cancela o pedido de desativação em andamento.

## Estrutura

```
modules/net-lock/
  expo-module.config.json
  index.ts
  android/src/main/
    AndroidManifest.xml
    res/xml/device_admin.xml
    res/xml/accessibility_config.xml
    java/expo/modules/netlock/
      NetLockModule.kt
      NetLockPolicy.kt
      NetLockAdminReceiver.kt
      NetLockAccessibilityService.kt
src/
  app/        telas: index.tsx e config.tsx
  components/ button.tsx
  hooks/      use-net-lock.ts
```
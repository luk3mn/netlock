# NetLock

App Android (Expo + módulo nativo em Kotlin) que fixa um **Private DNS** (como o do NextDNS) no aparelho e dificulta desligá-lo. Para desativar, é preciso pedir a desativação dentro do app e **esperar um tempo configurável**.

O projeto é de uso pessoal e instalado por APK, fora da Play Store.

## Branches

| Branch | Abordagem | Quando usar |
|---|---|---|
| `main` | Serviço de acessibilidade + Device Admin + `WRITE_SECURE_SETTINGS` | Funciona em qualquer aparelho já configurado, com contas. Apps bancários e o Shopee **acusam** o serviço de acessibilidade |
| `device-owner` | Device Owner (`DevicePolicyManager`), sem acessibilidade | Sem alertas de apps antifraude e com bloqueio feito pelo sistema. Exige aparelho **sem contas** na hora de virar Device Owner |

Este README cobre as duas. A `main` vem primeiro, e a `device-owner` tem a [sua própria seção](#branch-device-owner).

Os comandos usam o pacote da `main`, `com.netlock.app`. O pacote vem do `android.package` do `app.json` de cada branch, então confira o seu com:

```bash
adb shell pm list packages | grep -i netlock
```

Se o nome for outro, troque nos comandos. Na branch `device-owner` o pacote usado foi `com.luk3mn.netlock`.

## Como funciona (branch main)

| Peça | Função |
|---|---|
| `WRITE_SECURE_SETTINGS` | Permite ao app escrever o Private DNS e a lista de serviços de acessibilidade do sistema. Concedida uma vez via `adb` |
| Serviço de acessibilidade | Refaz o DNS na hora se alguém alterar e fecha as telas de Configurações que permitiriam desinstalar, forçar parada ou desligar o próprio serviço |
| Serviço de guarda | Serviço em primeiro plano que observa as configurações do sistema. Se o serviço de acessibilidade for desligado, ele o religa em cerca de um segundo |
| Device Admin | Bloqueia a desinstalação normal enquanto estiver ativo |
| Delay de desativação | Para desligar, é preciso pedir e esperar `DELAY_MS`. Depois abre uma janela de `WINDOW_MS` para confirmar |

Com a proteção ativa, desligar a acessibilidade pelas Configurações não funciona: o toggle volta a ligar sozinho. O caminho que exige esforço é plugar o celular no computador e revogar a permissão `WRITE_SECURE_SETTINGS` por `adb` (veja [Saídas de emergência](#saídas-de-emergência-branch-main)), ou passar pelo delay do app.

O contador usa `elapsedRealtime`, então mudar a data do aparelho não adianta o tempo. Reiniciar o aparelho **cancela** o pedido em andamento.

Nesta branch o app **não** é Device Owner, então sempre existe uma saída sem formatar o aparelho.

## Requisitos

- Android 9 ou superior (Private DNS) e depuração USB habilitada. A branch `device-owner` exige Android 11 ou superior
- Node.js, JDK 17 e Android SDK, com `adb` no PATH
- Um perfil no [NextDNS](https://nextdns.io) e o host da aba **Setup > Private DNS**, no formato `<id>.dns.nextdns.io`

## Instalação (branch main)

```bash
git clone <url-do-repositorio>
cd netlock
git switch main
npm install
```

Confirme o pacote no `app.json`:

```json
"android": {
  "package": "com.netlock.app"
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

Confirme que o app foi instalado com o nome esperado:

```bash
adb shell pm list packages | grep -i netlock
```

## Configuração inicial (branch main)

Faça nesta ordem.

### 1. Permissão de configurações seguras

```bash
adb shell pm grant com.netlock.app android.permission.WRITE_SECURE_SETTINGS
```

Confirme:

```bash
adb shell dumpsys package com.netlock.app | grep "WRITE_SECURE_SETTINGS: granted"
```

A saída deve ser `android.permission.WRITE_SECURE_SETTINGS: granted=true`. Se aparecer a permissão apenas listada, sem `granted=true`, ela foi solicitada mas **não** concedida.

No Samsung com **Auto Blocker**, desative antes "Bloquear comandos por cabo USB" em Configurações > Segurança e privacidade > Auto Blocker.

### 2. Administrador do dispositivo

No app, toque em **Conceder** ao lado de "Administrador do dispositivo" e confirme.

Se o botão não abrir a tela do sistema, ative manualmente em Configurações > Segurança e privacidade > Mais configurações de segurança > Apps administradores do dispositivo > netlock.

### 3. Serviço de acessibilidade

No app, toque em **Abrir** ao lado de "Serviço de acessibilidade". Em **Aplicativos instalados**, toque em **netlock**, ligue o toggle e confirme o aviso.

Se o toggle estiver bloqueado, abra Configurações > Apps > netlock > ⋮ > **Permitir configurações restritas** e tente de novo. Em apps instalados por `adb`, essa opção costuma nem aparecer, porque o Android não os marca como restritos.

### 4. Bateria

Configurações > Apps > netlock > Bateria > **Sem restrições**. Confirme também que o app não está em "Apps em suspensão" em Configurações > Bateria > Limites de uso em segundo plano.

Na tela de informações do app, mantenha **desligado** o toggle "Gerenciar app se não utilizado". Ele pode revogar permissões de apps parados.

### 5. Host do DNS e ativação

1. No app, abra **Configurações**, cole o host `<id>.dns.nextdns.io` e salve.
2. Volte à tela principal. Os três itens da checklist devem estar com ✓.
3. Toque em **Ativar proteção**.

Ao ativar, o serviço de guarda sobe em primeiro plano. Ele mostra uma notificação de baixa prioridade, mas continua rodando mesmo que as notificações do app estejam bloqueadas.

### 6. Validar

- [test.nextdns.io](https://test.nextdns.io) deve mostrar o seu perfil e o protocolo DoT.
- Mudar o DNS privado em Configurações deve ser revertido na hora.
- Desligar o netlock em Configurações > Acessibilidade deve religar o toggle em cerca de um segundo. Pelo terminal:

```bash
adb shell settings get secure enabled_accessibility_services
```

A saída deve continuar listando `com.netlock.app/expo.modules.netlock.NetLockAccessibilityService`.

## Uso diário

Com a proteção ativa:

- Alterar o DNS privado nas Configurações é revertido automaticamente.
- Desligar o serviço de acessibilidade é revertido automaticamente.
- Abrir a Info do app, tentar desinstalar, forçar a parada ou entrar na página de acessibilidade do app fecha a tela e volta à home.
- O host só pode ser alterado com a proteção desligada.

Para desativar:

1. Toque em **Pedir desativação** e confirme.
2. Aguarde o contador chegar a zero. Não reinicie o aparelho, senão o pedido é cancelado.
3. Quando aparecer "Liberado", toque em **Desativar agora** dentro da janela.

**Cancelar pedido** interrompe a espera a qualquer momento.

Ao desativar, o guarda para e você pode desligar a acessibilidade e o admin normalmente.

## Valores de teste e valores finais

Em `modules/net-lock/android/src/main/java/expo/modules/netlock/NetLockPolicy.kt`:

```kotlin
const val DELAY_MS = 2L * 60 * 1000
const val WINDOW_MS = 30L * 60 * 1000
```

Esses são os valores de **teste**: espera de 2 minutos e janela de 30. Antes de usar no dia a dia, troque por algo como:

```kotlin
const val DELAY_MS = 24L * 60 * 60 * 1000
const val WINDOW_MS = 2L * 60 * 60 * 1000
```

- `DELAY_MS`: espera entre pedir e poder desativar.
- `WINDOW_MS`: tempo que o botão "Desativar agora" fica disponível. Se passar, o pedido expira.

Atualize também o texto do alerta em `src/app/index.tsx`, que cita a janela. Com esperas longas, mantenha a janela generosa. Valide o ciclo completo com os valores de teste **antes** de trocá-los.

## Atualizar o app

Instale a nova versão **com a proteção desligada**:

```bash
npx expo run:android --variant release --device
```

A reinstalação por cima mantém o `WRITE_SECURE_SETTINGS`, mas pode desligar o serviço de acessibilidade. Se a checklist mostrar o item sem ✓, religue o serviço (etapa 3). Instale sempre pelo build local, para manter a mesma assinatura. Um APK assinado por outra chave não substitui a versão instalada, e com a proteção ativa você não consegue desinstalar a antiga.

Se vier de uma branch com `testOnly` (a `device-owner`), regenere a pasta nativa antes, senão o Android recusa a instalação com `INSTALL_FAILED_TEST_ONLY`:

```bash
npx expo prebuild --platform android --clean
grep -c testOnly android/app/src/main/AndroidManifest.xml
```

O `grep` deve imprimir `0` na `main`.

## Saídas de emergência (branch main)

O `adb uninstall` sozinho **não funciona** enquanto o Device Admin estiver ativo: o Android recusa com `DELETE_FAILED_INTERNAL_ERROR`. A ordem correta é desativar o admin antes de desinstalar.

### A. Pelo app

Pedir desativação, esperar o contador e tocar em **Desativar agora**. Depois siga o passo 3 abaixo.

### B. Pelo PC, sem esperar o delay

1. Revogue a permissão que permite ao guarda religar o serviço. Esta ordem importa:

```bash
adb shell pm revoke com.netlock.app android.permission.WRITE_SECURE_SETTINGS
```

2. Limpe a lista de acessibilidade (isso desliga também outros serviços de acessibilidade que você use, que podem ser religados depois):

```bash
adb shell settings put secure enabled_accessibility_services ""
adb shell settings put secure accessibility_enabled 0
```

3. Desative o admin em Configurações > Segurança e privacidade > Mais configurações de segurança > Apps administradores do dispositivo > netlock.

4. Desinstale e devolva o DNS ao modo automático:

```bash
adb uninstall com.netlock.app
adb shell settings put global private_dns_mode opportunistic
```

### C. Pelo próprio celular

Reinicie em **modo seguro** (segure o botão de desligar, toque e segure "Desligar" e confirme). Lá o serviço de acessibilidade não roda. Desative o admin (passo 3 acima) e desinstale o app.

Essas saídas existem de propósito, para que um bug não prenda o aparelho. O delay protege contra o impulso, não contra quem procura um atalho.

## Branch device-owner

Substitui a acessibilidade pelo **Device Owner**. O sistema passa a bloquear as alterações, e o app deixa de ter qualquer serviço de acessibilidade para os apps antifraude detectarem.

### O que muda

| | `main` | `device-owner` |
|---|---|---|
| Alerta de apps bancários e do Shopee | Sim | Não |
| DNS privado | Revertido pelo app | Bloqueado pelo sistema (`DISALLOW_CONFIG_PRIVATE_DNS`) |
| Desinstalar e forçar parada | Bloqueados pelo admin e por telas fechadas | Bloqueados pelo sistema |
| Data e hora | Delay imune por `elapsedRealtime` | Ajuste manual bloqueado, com horário automático ligado |
| Serviços extras | Acessibilidade e guarda | Nenhum |
| Pré-requisito | `pm grant` e permissões | Aparelho **sem contas** ao rodar o `set-device-owner` |
| Saída sem formatar | `adb` e modo seguro | `adb shell dpm remove-active-admin` |

O app é compilado como **`testOnly`** (por um config plugin em `plugins/with-test-only.js`). É isso que permite a saída pelo PC, sem formatar o aparelho. Por isso a instalação usa `adb install -t`.

### Pré-requisito: sem contas

O Android só aceita o `set-device-owner` num aparelho sem contas de usuário nem usuários extras (Pasta Segura). Veja o que existe:

```bash
adb shell dumpsys account | grep "Account {"
adb shell pm list users
```

O primeiro comando deve vir vazio, e o segundo deve listar só o usuário `0`. Contas do Google, da Samsung, da Adobe e do WhatsApp contam.

O truque de zerar `user_setup_complete` e `device_provisioned` antes do comando **não funcionou** no Samsung A05s (One UI): o erro `already some accounts on the device` continuou. O caminho prático é rodar o `set-device-owner` logo depois de **formatar** o aparelho, antes de adicionar qualquer conta, ou remover todas as contas por um tempo.

### Passo a passo

**0. Sair da `main`, se for o caso.** Com a proteção desligada:

1. Desligue a acessibilidade do netlock em Configurações > Acessibilidade.
2. Desative o admin em Configurações > Segurança e privacidade > Mais configurações de segurança > Apps administradores do dispositivo.
3. Desinstale: `adb uninstall com.netlock.app`.
4. Confirme: `adb shell pm list packages | grep -i netlock` não deve imprimir nada.

Se a proteção estiver ativa e você não quiser esperar o delay, use a [saída B](#saídas-de-emergência-branch-main) antes.

**1. Trocar de branch.**

```bash
git switch device-owner
npm install
```

Confirme que existe `plugins/with-test-only.js` e que o `app.json` o cita em `plugins`:

```json
"plugins": ["./plugins/with-test-only"]
```

Rode `npx tsc --noEmit`, que deve passar sem erros.

**2. Preparar o aparelho.** Deixe-o **sem contas** (veja o pré-requisito) e, no Samsung, desative o Auto Blocker ("Bloquear comandos por cabo USB") até o passo 5.

**3. Compilar.**

```bash
npx expo prebuild --platform android --clean
grep -n testOnly android/app/src/main/AndroidManifest.xml
cd android
./gradlew assembleRelease
cd ..
```

O `grep` deve mostrar `android:testOnly="true"`. Se não mostrar, o plugin não foi aplicado.

**4. Instalar.** O `run:android` não instala build `testOnly`, então use o `adb`:

```bash
adb install -t -r android/app/build/outputs/apk/release/app-release.apk
```

**5. Virar Device Owner.**

```bash
adb shell dpm set-device-owner com.luk3mn.netlock/expo.modules.netlock.NetLockAdminReceiver
adb shell dpm list-owners
```

O sucesso é `Success: Device owner set to package ...`.

**6. Configurar e ativar.** No app, abra **Configurações**, salve o host `<id>.dns.nextdns.io` e toque em **Ativar proteção**. O aviso amarelo de Device Owner deve ter sumido.

**7. Verificar.**

- Configurações > Conexões > Mais configurações de conexão > DNS privado: deve estar cinza, gerenciado pela organização.
- Data e hora: o ajuste manual deve estar bloqueado.
- Info do app: **Desinstalar** e **Forçar parada** devem estar cinza.
- [test.nextdns.io](https://test.nextdns.io) deve mostrar o seu perfil.

**8. Testar o ciclo e a saída** (com `DELAY_MS` de 2 minutos):

1. **Pedir desativação**, esperar e **Desativar agora**. Depois ative de novo.
2. Teste a saída pelo PC:

```bash
adb shell dpm remove-active-admin com.luk3mn.netlock/expo.modules.netlock.NetLockAdminReceiver
adb shell pm clear com.luk3mn.netlock
```

3. Repita os passos 5 e 6. Só depois disso adicione as contas, se for o caso.

**9. Valores finais.** Com a proteção **desligada**, troque `DELAY_MS` e `WINDOW_MS` (veja [Valores de teste e valores finais](#valores-de-teste-e-valores-finais)), recompile (passo 3) e reinstale com `adb install -t -r`. O Device Owner continua como estava.

### Saída (branch device-owner)

Com a depuração USB ligada, em qualquer momento:

```bash
adb shell dpm remove-active-admin com.luk3mn.netlock/expo.modules.netlock.NetLockAdminReceiver
```

Isso tira o Device Owner e libera tudo, sem formatar. O comando só funciona para builds `testOnly`, e em uma build que não seja `testOnly` ele falha com `Attempt to remove non-test admin`. Sem o PC, o caminho é o delay do app.

Atualizações do app só com a proteção desligada e sempre com `adb install -t -r`.

### Voltar para a main

Com a proteção desligada, remova o Device Owner pelo comando acima, desinstale o app (`adb uninstall com.luk3mn.netlock`), troque para a `main` e siga a instalação e a configuração dela.

## Solução de problemas

| Sintoma | Causa e correção |
|---|---|
| `Failure [package not found]` no `pm grant` | O pacote instalado não é o dos comandos. Veja com `adb shell pm list packages \| grep -i netlock`, confira se `adb devices` lista só o seu celular e use o nome real |
| `dumpsys` lista a permissão sem `granted=true` | A permissão foi solicitada, mas não concedida. Rode o `pm grant` com o pacote correto |
| `SecurityException` no `pm grant` | Auto Blocker da One UI ativo. Desative o bloqueio de comandos por USB |
| `Failure [DELETE_FAILED_INTERNAL_ERROR]` no `adb uninstall` | O Device Admin está ativo. Desative-o em Configurações antes de desinstalar |
| `Attempt to remove non-test admin` | O app instalado não é `testOnly`. Na `main`, desative o admin pelas Configurações. Na `device-owner`, instale a build `testOnly` |
| `INSTALL_FAILED_TEST_ONLY` | Sobrou `testOnly` no projeto nativo. Rode `prebuild --clean` ao instalar pela `main`, ou use `adb install -t -r` na `device-owner` |
| `already some accounts on the device` no `set-device-owner` | Há contas no aparelho. Veja com `adb shell dumpsys account \| grep "Account {"` |
| `already several users` no `set-device-owner` | Existe outro usuário (Pasta Segura, por exemplo). Apague-o |
| Botão **Conceder** não responde (`main`) | A tela de Device Admin não abre com `FLAG_ACTIVITY_NEW_TASK`. A função `requestAdmin` deve usar `appContext.currentActivity`. Ative manualmente pelo caminho da etapa 2 |
| Toggle da acessibilidade bloqueado (`main`) | Use "Permitir configurações restritas" na Info do app, se a opção aparecer |
| Toggle de acessibilidade não volta sozinho (`main`) | Confira se a proteção está ativa e se o guarda está de pé com `adb shell dumpsys activity services com.netlock.app`. Revise a bateria e confirme que o `WRITE_SECURE_SETTINGS` continua concedido |
| DNS não volta sozinho ao ser alterado (`main`) | O guarda ou o serviço de acessibilidade foi encerrado. Revise a bateria e reative a proteção |
| Uma tela sensível não fecha (`main`) | Os textos variam por idioma e versão da One UI. Ajuste a lista `sensitive` em `NetLockAccessibilityService.kt` |
| Sem internet após ativar | O Private DNS em modo `hostname` não tem fallback. Verifique o host, o perfil e o limite de consultas do plano no NextDNS |
| `Native module not found` ou `undefined is not a function` | O app instalado foi compilado antes de o código nativo mudar. Recompile e reinstale |
| `has no default export` ao rodar `tsc` | O `modules/net-lock/index.ts` perdeu a última linha, `export default requireNativeModule<NetLockModule>('NetLock');` |
| Erro `Conflicting overloads` ou `Unresolved reference 'Intent'` no build | Método duplicado ou import faltando após editar o Kotlin. Mantenha cada função uma única vez e importe `android.content.Intent` |

## Limitações

- **Branch main**: quem tem acesso ao `adb` ou ao modo seguro pode desfazer tudo. É o preço de não usar Device Owner.
- **Branch main**: apps bancários e antifraude (Inter, Shopee e outros) avisam ou restringem o uso quando há um serviço de acessibilidade de terceiros ativo, porque trojans bancários usam esse recurso. O serviço não pode ser ocultado desses apps. Alternativas: usar o app pelo navegador, testá-lo dentro da Pasta Segura da Samsung ou migrar para a branch `device-owner`.
- **Branch device-owner**: só funciona num aparelho sem contas na hora de virar Device Owner. A instalação exige `adb`.
- O filtro é por DNS. Navegadores ou apps com DNS próprio (DoH customizado) ignoram o Private DNS do sistema.
- O Private DNS depende de o host do NextDNS responder. Se ele parar, o aparelho fica sem resolver domínios até você desativar a proteção.
- Reiniciar o aparelho cancela o pedido de desativação em andamento.

## Estrutura

Branch `main`:

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
      GuardService.kt
      BootReceiver.kt
src/
  app/        telas: index.tsx e config.tsx
  components/ button.tsx
  hooks/      use-net-lock.ts
```

Branch `device-owner`: o mesmo, sem `accessibility_config.xml`, `NetLockAccessibilityService.kt`, `GuardService.kt` e `BootReceiver.kt`, e com `plugins/with-test-only.js` na raiz.
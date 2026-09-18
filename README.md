# Família Chat

App Android (Kotlin + Jetpack Compose) para comunicação privada só entre pais e filhos.

Não existe busca de contatos, lista de amigos ou qualquer forma de falar com mais
ninguém: o app conecta apenas os celulares que compartilham o mesmo código de
família de 6 dígitos, gerado uma única vez pelo responsável.

Projeto pessoal, criado para uso real dentro de uma família — não é um produto
comercial nem está publicado em lojas de aplicativo.

## Por quê

Pensado para crianças que ainda não devem ter WhatsApp, mas cujos pais querem uma
forma de comunicação direta e privada com elas — sem que a criança consiga falar
com estranhos, grupos ou qualquer contato fora da família.

## Funcionalidades

- **Pareamento fechado**: código de 6 dígitos gerado pelo responsável; sem
  e-mail, sem cadastro, sem descoberta de outros usuários.
- **Chat em tempo real** (texto, foto e áudio) só entre membros da mesma família.
- **PIN de 4 dígitos** para abrir o app em cada aparelho.
- **Notificações push de verdade** via Firebase Cloud Messaging, mesmo com o
  app fechado ou o celular bloqueado.
- **Mídia com validade**: fotos e áudios somem automaticamente depois de 7 dias
  (o histórico de texto continua para sempre). Existe pra manter o uso sempre
  dentro da cota gratuita do Firebase.

## Arquitetura

```
┌─────────────────┐         ┌──────────────────────────┐         ┌─────────────────┐
│  App Android     │ ◄─────► │  Firebase                 │ ◄─────► │  App Android     │
│  (pai)           │         │  • Auth (anônimo)         │         │  (filho)         │
│  Kotlin + Compose│         │  • Firestore (mensagens)  │         │  Kotlin + Compose│
└─────────────────┘         │  • Storage (fotos/áudios) │         └─────────────────┘
                             │  • Cloud Functions        │
                             │  • Cloud Messaging (push) │
                             └──────────────────────────┘
```

- **Firestore** guarda o pareamento e as mensagens, sincronizadas em tempo real.
- **Storage** guarda fotos e áudios enviados no chat.
- **Cloud Functions** (Node.js) fazem duas coisas:
  1. Disparam uma notificação push (FCM) para o outro membro sempre que chega
     mensagem nova.
  2. Rodam todo dia de madrugada apagando fotos/áudios com mais de 7 dias.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Firebase: Authentication, Firestore, Storage, Cloud Messaging, Cloud Functions
- Coil (carregamento de imagens)

## Pré-requisitos

1. [Android Studio](https://developer.android.com/studio) (mais recente estável)
2. Uma conta Google
3. [Node.js](https://nodejs.org/) 20+ (só para publicar as Cloud Functions)
4. Pelo menos 1 celular Android físico para testar (2 é melhor: "pai" e "filho")
5. Um cartão de crédito para cadastrar no plano **Blaze** do Firebase — as
   Cloud Functions e o Storage exigem esse plano, mas o uso real de uma família
   fica muito abaixo da cota gratuita (ver seção [Custo](#custo)).

## Como rodar

### 1. Criar o projeto no Firebase

1. Acesse o [console do Firebase](https://console.firebase.google.com) e crie
   um projeto novo.
2. Faça upgrade para o plano **Blaze**.
3. Em **Build > Authentication > Sign-in method**, ative o provedor **Anônimo**.
4. Em **Build > Firestore Database**, crie o banco (modo produção).
5. Em **Build > Storage**, clique em "Vamos começar" para criar o bucket padrão.
6. Adicione um app Android com o pacote `com.familiachat.app` e baixe o
   `google-services.json` gerado.
7. Coloque esse arquivo em `app/google-services.json`.

### 2. Publicar as regras e as funções

Com o [Firebase CLI](https://firebase.google.com/docs/cli) instalado e logado
(`firebase login`):

```bash
firebase deploy --only firestore:rules,storage,functions --project=<seu-project-id>
```

Isso publica:
- `firestore.rules` — regras de acesso do banco de dados
- `storage.rules` — regras de acesso das fotos/áudios
- `functions/index.js` — notificação push e limpeza automática de mídia

### 3. Abrir e rodar o app

1. Abra a pasta do projeto no Android Studio e espere o Gradle sincronizar.
2. Conecte um celular Android via USB com **Depuração USB** ativada.
3. Clique em **Run ▶**.

Repita a instalação no segundo celular (ou gere um APK em
**Build > Build APK(s)** e instale manualmente).

### 4. Testar

- No primeiro celular: "Sou o responsável: criar família" → anote o código.
- No segundo: "Recebi um código: entrar na família" → digite o código.
- Cada celular cria seu próprio PIN.
- Mensagens, fotos e áudios enviados em um aparecem no outro em tempo real,
  com notificação mesmo se o app estiver fechado.

## Estrutura do projeto

```
app/src/main/java/com/familiachat/app/
├── MainActivity.kt                 # navegação (pareamento → PIN → chat)
├── FamiliaChatApp.kt                # setup do canal de notificação
├── data/
│   ├── FamilyRepository.kt         # Firebase Auth + Firestore + Storage
│   ├── LocalPrefs.kt               # familyId e hash do PIN, só no aparelho
│   ├── Message.kt                  # modelo de mensagem (texto/foto/áudio)
│   ├── AudioRecorder.kt            # grava áudio (MediaRecorder)
│   ├── AudioPlayer.kt              # toca áudio (MediaPlayer)
│   └── ImageUtils.kt               # compressão de fotos antes do upload
├── ui/
│   ├── PairingScreen.kt            # criar/entrar em família
│   ├── PinSetupScreen.kt           # primeiro cadastro do PIN
│   ├── PinLockScreen.kt            # tela de PIN nas próximas aberturas
│   ├── ChatScreen.kt               # lista de mensagens, envio de texto/foto/áudio
│   └── components/PinDotsField.kt  # campo de PIN com bolinhas
└── service/
    ├── FcmService.kt               # recebe e mostra as notificações push
    └── ChatVisibility.kt           # evita notificar quando o chat já está aberto

functions/
└── index.js                        # push de mensagem nova + limpeza de mídia expirada
```

## Custo

Cálculo para uma família de até 5 pessoas com uso leve (texto todo dia + algumas
fotos/áudios):

- **Firestore, Authentication**: cota gratuita cobre uso normal, mesmo no Blaze.
- **Cloud Functions**: 2 milhões de execuções grátis por mês — uma família
  gastaria uma fração de 1% disso.
- **Cloud Messaging**: sempre grátis, sem limite.
- **Storage**: 5GB de armazenamento e 1GB/dia de download grátis. Com mídia
  expirando em 7 dias, o uso nunca deveria chegar perto disso.

Na prática, a chance de qualquer cobrança é essencialmente zero para esse uso.
Ainda assim, o plano Blaze **exige um cartão cadastrado** — é do Google, não é
possível contornar isso mantendo Cloud Functions e Storage.

## Limitações conhecidas

- **PIN**: guardado como hash SHA-256 em `SharedPreferences`. Funciona bem,
  mas dá para reforçar migrando para `EncryptedSharedPreferences`.
- **Múltiplos filhos**: o modelo de dados já suporta mais de 2 membros por
  família, mas a UI foi pensada para uma conversa única em grupo.
- **Sem status de entrega/leitura**: mensagens não mostram "enviado/lido".
- **Storage em região dos EUA**: para manter a cota gratuita, o bucket de
  mídia fica em `us-east1` (Firestore e Functions ficam em
  `southamerica-east1`) — implica uma latência extra pequena ao enviar fotos.

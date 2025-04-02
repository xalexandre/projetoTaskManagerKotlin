

# ✨ Task Manager

Um aplicativo de gerenciamento de tarefas desenvolvido em Kotlin, com recursos de autenticação, persistência offline e suporte multilinguagem.
APP Desenvolvido como projeto final da disciplina Integrações, Monetização e Publicação de Apps Kotlin - INFNET.


## 📱 Funcionalidades Principais

- 🔐 Autenticação de usuários (login/registro)
- ✅ Criação e gerenciamento de tarefas
- 🌐 Suporte para múltiplos idiomas (Português, Inglês, Espanhol)
- 🌙 Tema claro/escuro
- 📶 Funcionamento offline
- 🌤️ Integração com previsão do tempo
- 📸 Suporte para captura de imagens

## 🛠️ Tecnologias Utilizadas

- [Kotlin](https://kotlinlang.org/) - Linguagem principal
- [Firebase](https://firebase.google.com/)
  - Authentication
  - Realtime Database
  - Crashlytics
  - Analytics
  - Cloud Messaging
  - APP Distribution
- [Google Play Services](https://developers.google.com/android/guides/overview)
- [AdMob](https://admob.google.com/) - Monetização
- [Material Design](https://material.io/) - Interface do usuário

## 🔧 Configuração do Projeto

### Pré-requisitos

- Android Studio
- JDK 8 ou superior
- Google Play Services
- Conta Firebase

### Configuração Firebase

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com/)
2. Adicione seu aplicativo Android ao projeto Firebase
3. Baixe o arquivo `google-services.json` e coloque na pasta `app/`
4. Configure as regras do Realtime Database

## 📱 Recursos do App

### Autenticação
- Login com email/senha
- Registro de novos usuários
- Recuperação de senha

### Gerenciamento de Tarefas
- Criar novas tarefas
- Editar tarefas existentes
- Definir data e hora
- Adicionar descrições

### Funcionalidades Extras
- Persistência offline automática
- Sincronização em tempo real
- Suporte a múltiplos idiomas
- Temas claro/escuro
- Monitoramento de crashes e analytics

## 🚀 Como Executar

1. Clone o repositório
```bash
git clone (https://github.com/xalexandre/projetoTaskManagerKotlin)
```

2. Abra o projeto no Android Studio

3. Sincronize o projeto com Gradle Files

4. Execute o aplicativo em um emulador ou dispositivo físico

## 🔒 Segurança

O aplicativo implementa várias camadas de segurança:
- Firebase App Check
- Autenticação segura
- Verificação de Play Integrity
- Persistência segura de dados

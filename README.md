# 🎙️ Gravador de Áudio JavaFX (Streaming & Chunking)

Um cliente desktop Windows desenvolvido em Java (JavaFX) que captura áudio do microfone, segmenta a gravação em arquivos de 10 segundos em tempo real e permite a criação de um instalador nativo (`.msi`) autossuficiente.

## 🚀 Funcionalidades

* **Interface Gráfica (GUI):** Construída com JavaFX, simples e responsiva.
* **Gravação em Tempo Real:** O áudio é processado e salvo enquanto é gravado, sem armazenar tudo na memória RAM.
* **Chunking Automático:** O áudio é dividido automaticamente em arquivos `.wav` a cada **10 segundos**.
* **Multithreading:**
    * Thread 1: Interface Gráfica (não trava nunca).
    * Thread 2: Captura de áudio do microfone.
    * Thread 3+: Salvamento de arquivos em disco (I/O).
* **Instalador Nativo:** Gera um arquivo `.msi` que instala o programa e um JRE embutido (o usuário final não precisa ter Java instalado).

---

## 📂 Onde os Áudios são Salvos?

Por padrão, o aplicativo salva as gravações na pasta de **Músicas** do usuário, para evitar problemas de permissão no Windows.

**Diretório:** `C:\Users\[SeuUsuario]\Minhas Gravacoes\`

---

## 🛠️ Pré-requisitos de Desenvolvimento

Para compilar e rodar este projeto, você precisa de:

1.  **Java JDK 21+:** Recomendado [Azul Zulu JDK](https://www.azul.com/downloads/) ou similar.
2.  **Maven:** Para gerenciamento de dependências.
3.  **WiX Toolset v3.11 (CRÍTICO):**
    * Necessário **apenas** para gerar o instalador `.msi`.
    * ⚠️ **Atenção:** O `jpackage` do Java **não é compatível** com WiX v4 ou v5. Você **deve** instalar a versão 3.x.
    * **Download:** [WiX Toolset v3.14](https://github.com/wixtoolset/wix3/releases/tag/wix314rtm)
    * Certifique-se de que o caminho `C:\Program Files (x86)\WiX Toolset v3.11\bin` está nas suas **Variáveis de Ambiente (PATH)**.

---

## ▶️ Como Rodar (Modo Desenvolvimento)

Para testar a aplicação rapidamente sem criar o instalador:

```bash
mvn clean javafx:run

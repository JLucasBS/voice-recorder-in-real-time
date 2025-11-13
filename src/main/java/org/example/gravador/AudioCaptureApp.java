package org.example.gravador;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class AudioCaptureApp extends Application {

    private Button recordButton;

    // Usamos 'volatile' para garantir que a mudança seja vista por todas as threads
    private volatile boolean isRecording = false;

    // Componentes de Áudio
    private AudioFormat audioFormat;
    private TargetDataLine targetDataLine;
    private Thread captureThread;

    // Duração de cada "fatia" de áudio em segundos
    private static final int CHUNK_DURATION_SECONDS = 10;

    // Pasta para salvar os áudios
    private File saveDirectory;

    private AudioFormat getAudioFormat() {
        float sampleRate = 44100.0F;
        int sampleSizeInBits = 16;
        int channels = 1; // Mono
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Gravador de Áudio (Streaming)");

        recordButton = new Button("▶️ Iniciar Gravação");
        recordButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");

        // Configura o diretório de salvamento (Ex: C:\Users\SeuNome\Minhas Gravacoes)
        String userHome = System.getProperty("user.home");
        saveDirectory = new File(userHome, "Minhas Gravacoes");
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        System.out.println("Salvando áudios em: " + saveDirectory.getAbsolutePath());

        recordButton.setOnAction(e -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        StackPane root = new StackPane();
        root.getChildren().add(recordButton);
        Scene scene = new Scene(root, 350, 200);
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(event -> {
            stopRecording();
        });

        primaryStage.show();
    }

    /**
     * Inicia a captura e o fatiamento em tempo real.
     */
    private void startRecording() {
        try {
            audioFormat = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Linha de áudio não suportada.");
                return;
            }

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            isRecording = true;
            recordButton.setText("⏹️ Parar Gravação");

            // --- LÓGICA DE CAPTURA E FATIAMENTO EM TEMPO REAL ---

            captureThread = new Thread(() -> {
                try {
                    // 1. Calcular o tamanho exato de um "balde" (chunk)
                    int bytesPerSecond = (int) (audioFormat.getFrameRate() * audioFormat.getFrameSize());
                    int bytesPerChunk = bytesPerSecond * CHUNK_DURATION_SECONDS;
                    byte[] buffer = new byte[4096]; // Buffer de leitura
                    int chunkCount = 0;

                    // 2. Loop principal: continua enquanto 'isRecording' for verdadeiro
                    while (isRecording) {
                        chunkCount++;
                        ByteArrayOutputStream currentChunkBAOS = new ByteArrayOutputStream();
                        int bytesInCurrentChunk = 0;
                        String filename = "gravacao_" + System.currentTimeMillis() + "_chunk" + chunkCount + ".wav";

                        System.out.println("Iniciando novo chunk: " + filename);

                        // 3. Loop interno: enche um "balde" (chunk)
                        while (isRecording && bytesInCurrentChunk < bytesPerChunk) {

                            // Calcula quanto falta para encher o balde
                            int bytesToRead = Math.min(buffer.length, bytesPerChunk - bytesInCurrentChunk);

                            // Lê do microfone
                            int bytesRead = targetDataLine.read(buffer, 0, bytesToRead);

                            if (bytesRead > 0) {
                                currentChunkBAOS.write(buffer, 0, bytesRead);
                                bytesInCurrentChunk += bytesRead;
                            }
                        } // Fim do loop interno (balde cheio)

                        // 4. Se tivermos algo no balde, mandamos salvar
                        if (currentChunkBAOS.size() > 0) {
                            // Pega os bytes e INICIA UMA NOVA THREAD SÓ PARA SALVAR
                            final byte[] chunkData = currentChunkBAOS.toByteArray();
                            final String finalFilename = filename;

                            new Thread(() -> {
                                saveChunkToFile(chunkData, finalFilename);
                            }).start();
                        }
                    } // Fim do loop principal (usuário clicou em parar)

                } finally {
                    // 5. Limpeza: Garante que a linha e o botão sejam resetados
                    //    Este 'finally' executa quando o loop 'while(isRecording)' termina
                    targetDataLine.stop();
                    targetDataLine.close();
                    System.out.println("Captura finalizada.");

                    Platform.runLater(() -> {
                        recordButton.setText("▶️ Iniciar Gravação");
                        recordButton.setDisable(false);
                    });
                }
            });

            captureThread.start();
            System.out.println("Gravação em tempo real iniciada...");

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            System.err.println("Microfone não disponível ou em uso.");
        }
    }

    /**
     * Apenas sinaliza para a thread de captura parar.
     * A thread de captura é responsável por se limpar.
     */
    private void stopRecording() {
        if (!isRecording) return; // Já está parando

        isRecording = false; // Sinaliza para a thread parar

        // Atualiza a UI imediatamente
        recordButton.setText("⏹️ Finalizando...");
        recordButton.setDisable(true); // Desabilita até a thread confirmar que parou

        System.out.println("Sinal de parada enviado...");
        // Não precisamos mais do captureThread.join() aqui,
        // pois a própria thread vai se limpar e reabilitar o botão.
    }

    /**
     * Função auxiliar que salva um array de bytes (uma fatia) em um arquivo .wav.
     * Esta função é chamada em sua própria thread.
     */
    private void saveChunkToFile(byte[] audioData, String filename) {
        try {
            File audioFile = new File(saveDirectory, filename);

            // Usa try-with-resources para fechar os streams automaticamente
            try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                 AudioInputStream audioInputStream = new AudioInputStream(
                         bais,
                         audioFormat,
                         audioData.length / audioFormat.getFrameSize())) {

                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, audioFile);
                System.out.println("Fatia salva em: " + audioFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar o chunk: " + filename);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
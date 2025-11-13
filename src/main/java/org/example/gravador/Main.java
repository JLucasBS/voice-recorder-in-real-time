package org.example.gravador;

/**
 * Classe "Lançadora" (Launcher).
 * Esta classe é usada para "enganar" o Java e permitir a criação de um
 * Fat JAR que funciona com o JavaFX modular.
 */
public class Main {
    public static void main(String[] args) {
        // A única função desta classe é chamar o main da sua classe de aplicação
        AudioCaptureApp.main(args);
    }
}
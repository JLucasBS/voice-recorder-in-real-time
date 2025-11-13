module gravador.javafx { // O nome do módulo pode ser o que você quiser

    // 1. Permite que o JavaFX funcione
    requires javafx.controls;

    // 2. Permite o acesso ao "javax.sound.sampled" (biblioteca de áudio)
    requires java.desktop;

    // 3. Expõe seu pacote para o JavaFX poder lançar a aplicação
    //    (Mude para o nome do seu pacote)
    exports org.example.gravador;
}
package model;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Fluxo {
    public static void main(String[] args) throws Exception {
        InetAddress ipDestino = InetAddress.getByName("127.0.0.1");
        int portaDestino = 5000;

        // Instancia o receptor com lógica completa (ele mesmo cria o socket internamente)
        Receptor receptor = new Receptor();
        Thread receptorThread = new Thread(() -> receptor.iniciar(portaDestino));
        receptorThread.start();

        Thread.sleep(500); // garante que o receptor está escutando

        // Cria socket do emissor com porta aleatória (ou fixa, se quiser)
        DatagramSocket socketEmissor = new DatagramSocket();
        Emissor emissor = new Emissor(ipDestino, portaDestino);
        var mensagens = Arrays.asList("Mensagem 1", "Mensagem 2", "Mensagem 3");

        // Inicia envio com controle de janela e timeout
        emissor.iniciarProcessoDeEnvio(mensagens, socketEmissor);

        // Espera finalização do receptor (opcional, útil para testes)
        receptorThread.join();
    }
}

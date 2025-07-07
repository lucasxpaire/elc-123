package model;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Fluxo {
    public static void main(String[] args) throws Exception {
        InetAddress ipDestino = InetAddress.getByName("127.0.0.1");
        int portaDestino = 5000;

        // Socket do receptor escutando na porta 5000
        DatagramSocket socketReceptor = new DatagramSocket(portaDestino);

        // Socket do emissor com porta aleatória (ou uma porta específica se quiser)
        DatagramSocket socketEmissor = new DatagramSocket();

        Receptor receptor = new Receptor();
        Thread receptorThread = new Thread(() -> receptor.iniciarRecepcao(socketReceptor));
        receptorThread.start();

        Thread.sleep(500); // garante que o receptor está pronto

        Emissor emissor = new Emissor(ipDestino, portaDestino);
        var mensagens = Arrays.asList("Mensagem 1", "Mensagem 2", "Mensagem 3");
        emissor.iniciarProcessoDeEnvio(mensagens, socketEmissor);

        receptorThread.join(); // espera o receptor (opcional)
    }
}

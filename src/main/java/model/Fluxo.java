package model;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class Fluxo {
    public static void main(String[] args) throws Exception {
        InetAddress ipDestino = InetAddress.getByName("127.0.0.1");
        int portaDestino = 5000;
        Random rand = new Random();

        while (true) {
            DatagramSocket socket = new DatagramSocket();
            Emissor emissor = new Emissor(ipDestino, portaDestino);

            int tamanho = 1 + rand.nextInt(4); // 1 a 4 bytes
            byte[] dados = new byte[tamanho];
            rand.nextBytes(dados);

            boolean inserirErro = rand.nextDouble() < 0.2;
            if (inserirErro) {
                int byteIdx = rand.nextInt(tamanho);
                int bitIdx = rand.nextInt(8);
                dados[byteIdx] ^= (1 << bitIdx);
                System.out.println("Fluxo: Mensagem gerada com ERRO proposital.");
            } else {
                System.out.println("Fluxo: Mensagem gerada CORRETA.");
            }

            emissor.iniciarProcessoDeEnvioBytes(
                java.util.Collections.singletonList(dados), socket
            );

            socket.close();
            Thread.sleep(5000);
        }
    }
}
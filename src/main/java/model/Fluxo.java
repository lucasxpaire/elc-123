package model;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class Fluxo {
    public static void main(String[] args) throws Exception {
        InetAddress ipDestino = InetAddress.getByName("127.0.0.1");
        int portaDestino = 5000;
        Random rand = new Random();

        int portaLocalEmissor = 6000; 
        DatagramSocket socket = new DatagramSocket(portaLocalEmissor);
        Emissor emissor = new Emissor(ipDestino, portaDestino);

        while (true) {
            int tamanho = 1 + rand.nextInt(4); // 1 a 4 bytes
            byte[] dados = new byte[tamanho];
            rand.nextBytes(dados);

            boolean inserirErro = rand.nextDouble() < 0.5; // 50% de chance de erro

            if (inserirErro) {
                emissor.enviarMensagemCorrompida(socket);
            } else {
                // Envio correto com CRC anexado
                byte crc = CRC.calcularCrc(dados);
                byte[] dadosComCrc = new byte[tamanho + 1];
                System.arraycopy(dados, 0, dadosComCrc, 0, tamanho);
                dadosComCrc[tamanho] = crc;

                System.out.println("Fluxo: Mensagem gerada CORRETA.");
                emissor.iniciarProcessoDeEnvioBytes(
                    java.util.Collections.singletonList(dadosComCrc), socket
                );
            }

            Thread.sleep(5000);
        }

    }
}

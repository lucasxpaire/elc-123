package model;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Fluxo {
    public static void main(String[] args) throws Exception {
        InetAddress ipDestino = InetAddress.getByName("127.0.0.1");
        int portaDestino = 5000;
        Random rand = new Random();

        while (true) {
            int portaLocalEmissor = 6000; 
            try (DatagramSocket socket = new DatagramSocket(portaLocalEmissor)) {
                Emissor emissor = new Emissor(ipDestino, portaDestino);

                int quantidadeDeQuadros = 1 + rand.nextInt(3); // de 1 a 3 quadros
                List<byte[]> listaDeMensagens = new ArrayList<>();

                for (int i = 0; i < quantidadeDeQuadros; i++) {
                    int tamanho = 1 + rand.nextInt(4); // de 1 a 4 bytes
                    byte[] dados = new byte[tamanho];
                    rand.nextBytes(dados);

                    boolean inserirErro = rand.nextDouble() < 0.5;

                    if (inserirErro) {
                        // Cria dados com CRC, depois corrompe 1 bit
                        byte crc = CRC.calcularCrc(dados);
                        byte[] dadosComCrc = new byte[tamanho + 1];
                        System.arraycopy(dados, 0, dadosComCrc, 0, tamanho);
                        dadosComCrc[tamanho] = crc;

                        // Corrompe bit aleatório
                        int byteIndex = rand.nextInt(dadosComCrc.length);
                        int bitIndex = rand.nextInt(8);
                        dadosComCrc[byteIndex] ^= (1 << bitIndex);

                        listaDeMensagens.add(dadosComCrc);
                        System.out.println("Fluxo: Quadro " + i + " gerado CORROMPIDO (bit alterado).");
                    } else {
                        // Dados corretos com CRC
                        byte crc = CRC.calcularCrc(dados);
                        byte[] dadosComCrc = new byte[tamanho + 1];
                        System.arraycopy(dados, 0, dadosComCrc, 0, tamanho);
                        dadosComCrc[tamanho] = crc;

                        listaDeMensagens.add(dadosComCrc);
                        System.out.println("Fluxo: Quadro " + i + " gerado CORRETO.");
                    }
                }

                System.out.println("Fluxo: Enviando " + quantidadeDeQuadros + " quadros.");
                emissor.iniciarProcessoDeEnvioBytes(listaDeMensagens, socket);

                // Fecha o socket explicitamente 
                socket.close();

            } catch (Exception e) {
                System.err.println("Erro no fluxo: " + e.getMessage());
            }

            Thread.sleep(5000);
        }
    }
}

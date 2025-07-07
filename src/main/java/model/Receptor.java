package model;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class Receptor {

    public void iniciarRecepcao(DatagramSocket socket) {
        byte[] buffer = new byte[1024];
        System.out.println("Receptor aguardando pacotes...");
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] dados = Arrays.copyOf(packet.getData(), packet.getLength());
                System.out.println("Recebido pacote de " + packet.getAddress() + ":" + packet.getPort());
                System.out.print("Dados recebidos (brutos): ");
                imprimirBits(dados);

                // Etapa 1: Remover bit stuffing
                byte[] dadosDesstuffados = Quadro.removerBitStuffing(dados);

                // Etapa 2: Verificar CRC
                boolean valido = CRC.verificarCRC(dadosDesstuffados);

                System.out.println("Dados após remover bit stuffing:");
                imprimirBits(dadosDesstuffados);
                System.out.println("CRC válido? " + valido);
                System.out.println("--------------------------------------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void imprimirBits(byte[] bytes) {
        if (bytes == null) {
            System.out.println("null");
            return;
        }
        for (byte b : bytes) {
            for (int i = 7; i >= 0; i--) {
                System.out.print(((b >> i) & 1));
            }
            System.out.print(" ");
        }
        System.out.println();
    }
}

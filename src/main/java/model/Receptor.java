package model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receptor {
    // Configurações do Protocolo
    private static final int BITS_PARA_NUMERO_DE_SEQUENCIA = 3;
    private static final int NUM_MAX_SEQ = (int) Math.pow(2, BITS_PARA_NUMERO_DE_SEQUENCIA);
    private static final int TAMANHO_BUFFER = 2048; // Um pouco maior para acomodar quadros com stuffing

    // Endereçamento
    private static final byte ENDERECO_EMISSOR = 1;
    private static final byte ENDERECO_RECEPTOR = 2;

    // Estado do Protocolo
    private int proximoNumeroEsperado; // Rn

    // Componentes de Rede
    private DatagramSocket socket;
    private InetAddress ipEmissor;
    private int portaEmissor;

    public Receptor() {
        this.proximoNumeroEsperado = 0;
    }

    /**
     * Inicia o processo de escuta e recebimento de quadros.
     */
    public void iniciar(int portaLocal) {
        try {
            socket = new DatagramSocket(portaLocal);
            System.out.println("RECEPTOR: Escutando na porta " + portaLocal + "...");

            while (true) {
                byte[] buffer = new byte[TAMANHO_BUFFER];
                DatagramPacket pacoteRecebido = new DatagramPacket(buffer, buffer.length);

                socket.receive(pacoteRecebido);

                this.ipEmissor = pacoteRecebido.getAddress();
                this.portaEmissor = pacoteRecebido.getPort();

                processarPacoteRecebido(pacoteRecebido);
            }
        } catch (IOException e) {
            System.err.println("Erro de I/O no receptor: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void processarPacoteRecebido(DatagramPacket pacote) throws IOException {
        byte[] dadosRecebidos = new byte[pacote.getLength()];
        System.arraycopy(pacote.getData(), pacote.getOffset(), dadosRecebidos, 0, pacote.getLength());

        Quadro quadro = Quadro.reconstruirQuadro(dadosRecebidos);

        if (quadro == null) {
            System.err.println("RECEPTOR: Quadro recebido corrompido ou mal formado. Descartando.");
            return;
        }

        System.out.println("RECEPTOR: Recebeu quadro com SeqNum: " + quadro.getNumeroSequencia() + ". Esperando por: " + proximoNumeroEsperado);

        if (quadro.getNumeroSequencia() == proximoNumeroEsperado) {
            //String dadosRecebidos = new String(quadro.getDados());
            System.out.println("RECEPTOR: Quadro aceito. Dados: \"" + quadro.getDados().length + "\"");

            proximoNumeroEsperado = (proximoNumeroEsperado + 1) % NUM_MAX_SEQ;

            enviarAck(proximoNumeroEsperado);
        } else {
            System.err.println("RECEPTOR: Quadro fora de ordem. Descartando. Enviando ACK para " + proximoNumeroEsperado);
            enviarAck(proximoNumeroEsperado);
        }
    }

    private void enviarAck(int ackNum) {
        Quadro quadroAck = new Quadro(ENDERECO_RECEPTOR, ENDERECO_EMISSOR, (byte) ackNum, Quadro.TIPO_CONFIRMACAO, new byte[0]);
        System.out.println("RECEPTOR: Enviando ACK para o próximo quadro esperado: " + ackNum);

        try {
            byte[] dadosDoAck = quadroAck.montarQuadroParaTransmissao();
            DatagramPacket pacoteAck = new DatagramPacket(dadosDoAck, dadosDoAck.length, ipEmissor, portaEmissor);
            socket.send(pacoteAck);
        } catch (IOException e) {
            System.err.println("Erro ao enviar ACK: " + e.getMessage());
        }
    }
}
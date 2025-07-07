package model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Emissor {
    // --- Configurações do Protocolo ---
    private static final int BITS_PARA_NUMERO_DE_SEQUENCIA = 3;
    private static final int NUM_MAX_SEQ = (int) Math.pow(2, BITS_PARA_NUMERO_DE_SEQUENCIA);
    private static final int TAMANHO_JANELA = NUM_MAX_SEQ - 1;
    private static final int TIMEOUT_MS = 3000;
    private static final int TAMANHO_BUFFER_ACK = 1024;

    // --- Endereçamento ---
    private static final byte ENDERECO_EMISSOR = 1;
    private static final byte ENDERECO_RECEPTOR = 2;

    // --- Estado do Protocolo ---
    private int baseDaJanela;                  // Sf: Início da janela (número do quadro mais antigo não confirmado).
    private int proximoNumeroDeSequencia;      // Sn: Próximo número de sequência a ser usado.
    private final Lock janelaLock = new ReentrantLock(); // Trava para acesso seguro à janela.

    // --- Estruturas de Dados ---
    private final List<Quadro> janelaDeEnvio; // Buffer para os quadros enviados mas não confirmados.
    private final Timer[] timers;             // Um timer para cada número de sequência.

    // --- Componentes de Rede ---
    private DatagramSocket socket;
    private InetAddress ipDestino;
    private int portaDestino;

    public Emissor(InetAddress ipDestino, int portaDestino) {
        this.ipDestino = ipDestino;
        this.portaDestino = portaDestino;

        // Inicializa o estado do protocolo
        this.baseDaJanela = 0;
        this.proximoNumeroDeSequencia = 0;
        this.janelaDeEnvio = new ArrayList<>();
        this.timers = new Timer[NUM_MAX_SEQ];
    }

    public void iniciarProcessoDeEnvio(List<String> mensagens, DatagramSocket socket) {
        this.socket = socket;
        iniciarEscutaDeACKS();

        while (baseDaJanela < mensagens.size()) {
            janelaLock.lock();
            try {
                // Envia novos quadros enquanto a janela tiver espaço e houver mensagens
                if (proximoNumeroDeSequencia < baseDaJanela + TAMANHO_JANELA && proximoNumeroDeSequencia < mensagens.size()) {
                    enviarNovoQuadro(mensagens.get(proximoNumeroDeSequencia));
                }
            } finally {
                janelaLock.unlock();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread principal do emissor foi interrompida.");
            }
        }
        System.out.println("\nTodas as mensagens foram enviadas e confirmadas. Processo encerrado.");
    }

    private void enviarNovoQuadro(String dados) {
        byte seqNum = (byte) (proximoNumeroDeSequencia % NUM_MAX_SEQ);
        Quadro quadro = new Quadro(ENDERECO_EMISSOR, ENDERECO_RECEPTOR, seqNum, Quadro.TIPO_DADOS, dados.getBytes());

        janelaDeEnvio.add(quadro);
        System.out.println("EMISSOR: Enviando quadro com SeqNum: " + seqNum);
        enviarParaRede(quadro);
        iniciarTimer(seqNum);
        proximoNumeroDeSequencia++;
    }

    /**
     * Inicia uma thread para escutar por pacotes de ACK vindos do receptor.
     */
    private void iniciarEscutaDeACKS() {
        Thread ackListenerThread = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    byte[] buffer = new byte[TAMANHO_BUFFER_ACK];
                    DatagramPacket pacoteRecebido = new DatagramPacket(buffer, buffer.length);
                    socket.receive(pacoteRecebido);

                    Quadro quadroAck = Quadro.reconstruirQuadro(pacoteRecebido.getData());
                    if (quadroAck != null && quadroAck.getTipo() == Quadro.TIPO_CONFIRMACAO) {
                        processarACK(quadroAck.getNumeroSequencia());
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("Erro de I/O ao receber ACK: " + e.getMessage());
                }
            }
        });
        ackListenerThread.setDaemon(true);
        ackListenerThread.start();
    }

    /**
     * Processa um ACK recebido, deslizando a janela de envio se o ACK for válido.
     */
    private void processarACK(int ackNum) {
        janelaLock.lock();
        try {
            System.out.println("\nEMISSOR: Recebeu ACK " + ackNum + ". Base atual: " + (baseDaJanela % NUM_MAX_SEQ));

            if (verificarACKDentroDaJanela(ackNum)) {
                // Desliza a base da janela até o número de sequência do ACK
                while ((baseDaJanela % NUM_MAX_SEQ) != ackNum) {
                    pararTimer(baseDaJanela % NUM_MAX_SEQ);
                    if (!janelaDeEnvio.isEmpty()) {
                        janelaDeEnvio.remove(0);
                    }
                    baseDaJanela++;
                }
                System.out.println("EMISSOR: Janela deslizou. Nova base: " + (baseDaJanela % NUM_MAX_SEQ) + "\n");
            } else {
                System.out.println("EMISSOR: ACK " + ackNum + " é duplicado ou antigo. Ignorando.\n");
            }
        } finally {
            janelaLock.unlock();
        }
    }

    /**
     * Verifica se um número de ACK recebido está dentro da janela de envio atual,
     * tratando corretamente o caso de "wrap-around" dos números de sequência.
     */
    private boolean verificarACKDentroDaJanela(int ackNum) {
        int baseSeq = baseDaJanela % NUM_MAX_SEQ;
        int proximoSeq = proximoNumeroDeSequencia % NUM_MAX_SEQ;

        // Caso normal (ex: base=2, proximo=5, janela=[2,3,4]). ACKs válidos: 3, 4, 5.
        if (baseSeq < proximoSeq) {
            return ackNum > baseSeq && ackNum <= proximoSeq;
        }
        // Caso com "wrap-around" (ex: base=6, proximo=2, janela=[6,7,0,1]). ACKs válidos: 7, 0, 1, 2.
        else {
            return ackNum > baseSeq || ackNum <= proximoSeq;
        }
    }

    /**
     * Lida com o evento de timeout, reenviando todos os quadros na janela.
     */
    private void lidarComTimeOut() {
        janelaLock.lock();
        try {
            System.err.println("\nTIMEOUT! Reenviando toda a janela a partir da base: " + (baseDaJanela % NUM_MAX_SEQ) + "\n");
            // Go-Back-N: Reenvia todos os quadros da janela.
            for (int i = 0; i < janelaDeEnvio.size(); i++) {
                Quadro quadroParaReenviar = janelaDeEnvio.get(i);
                int seqNumNoQuadro = (baseDaJanela + i) % NUM_MAX_SEQ;
                System.err.println("EMISSOR (REENVIO): Enviando quadro com SeqNum: " + seqNumNoQuadro);
                enviarParaRede(quadroParaReenviar);
                iniciarTimer(seqNumNoQuadro);
            }
        } finally {
            janelaLock.unlock();
        }
    }

    /**
     * Envia um quadro pela rede usando o socket.
     */
    private void enviarParaRede(Quadro quadro) {
        try {
            byte[] dadosDoQuadro = quadro.montarQuadroParaTransmissao();
            DatagramPacket pacoteParaEnviar = new DatagramPacket(dadosDoQuadro, dadosDoQuadro.length, ipDestino, portaDestino);
            socket.send(pacoteParaEnviar);
        } catch (IOException e) {
            System.err.println("Erro de I/O ao enviar quadro: " + e.getMessage());
        }
    }

    private void iniciarTimer(int seqNum) {
        pararTimer(seqNum); // Garante que não há um timer antigo rodando
        timers[seqNum] = new Timer(true);
        timers[seqNum].schedule(new TimerTask() {
            @Override
            public void run() {
                lidarComTimeOut();
            }
        }, TIMEOUT_MS);
    }

    private void pararTimer(int seqNum) {
        if (timers[seqNum] != null) {
            timers[seqNum].cancel();
            timers[seqNum] = null;
        }
    }
}
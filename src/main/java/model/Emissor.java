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
    private static final int BITS_PARA_NUMERO_DE_SEQUENCIA = 3;
    private static final int NUM_MAX_SEQ = (int) Math.pow(2, BITS_PARA_NUMERO_DE_SEQUENCIA);
    private static final int TAMANHO_JANELA = NUM_MAX_SEQ - 1;
    private static final int TIMEOUT_MS = 3000;
    private static final int TAMANHO_BUFFER_ACK = 1024;

    private static final byte ENDERECO_EMISSOR = 1;
    private static final byte ENDERECO_RECEPTOR = 2;

    private int baseDaJanela;
    private int proximoNumeroDeSequencia;
    private final Lock janelaLock = new ReentrantLock();

    private final List<Quadro> janelaDeEnvio;
    private final Timer[] timers;

    private DatagramSocket socket;
    private InetAddress ipDestino;
    private int portaDestino;

    private static final int MAX_TENTATIVAS = 3;
    private final int[] tentativasPorSeqNum = new int[NUM_MAX_SEQ];

    private boolean houveFalha = false;

    public Emissor(InetAddress ipDestino, int portaDestino) {
        this.ipDestino = ipDestino;
        this.portaDestino = portaDestino;
        this.baseDaJanela = 0;
        this.proximoNumeroDeSequencia = 0;
        this.janelaDeEnvio = new ArrayList<>();
        this.timers = new Timer[NUM_MAX_SEQ];
    }


    private void resetarEstado() {
        baseDaJanela = 0;
        proximoNumeroDeSequencia = 0;
        janelaDeEnvio.clear();
        houveFalha = false;

        for (int i = 0; i < NUM_MAX_SEQ; i++) {
            tentativasPorSeqNum[i] = 0;
            pararTimer(i);
        }
    }

    // No Emissor.java
    public void iniciarProcessoDeEnvioBytes(List<byte[]> mensagens, DatagramSocket socket) {
        this.socket = socket;
        //resetarEstado();
        iniciarEscutaDeACKS();

        while (baseDaJanela < mensagens.size()) {
            janelaLock.lock();
            try {
                if (proximoNumeroDeSequencia < baseDaJanela + TAMANHO_JANELA &&
                    proximoNumeroDeSequencia < mensagens.size()) {
                    enviarNovoQuadroBytes(mensagens.get(proximoNumeroDeSequencia));
                }
            } finally {
                janelaLock.unlock();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (houveFalha) {
            System.out.println("\n\nAlguma mensagem FALHOU após o número máximo de tentativas. Processo encerrado.\n");
        } else {
            System.out.println("\n\nTodas as mensagens foram enviadas e confirmadas. Processo encerrado.\n");
        }
    }

    private void enviarNovoQuadroBytes(byte[] dados) {
        byte seqNum = (byte) (proximoNumeroDeSequencia % NUM_MAX_SEQ);
        Quadro quadro = new Quadro(ENDERECO_EMISSOR, ENDERECO_RECEPTOR, seqNum, Quadro.TIPO_DADOS, dados);
        tentativasPorSeqNum[seqNum] = 0;
        janelaDeEnvio.add(quadro);
        System.out.println("EMISSOR: Enviando quadro com SeqNum: " + seqNum);
        enviarParaRede(quadro);
        iniciarTimer(seqNum);
        proximoNumeroDeSequencia++;
    }

     public void iniciarProcessoDeEnvio(List<String> mensagens, DatagramSocket socket) {
        this.socket = socket;
        resetarEstado(); // <-- corrigido

        iniciarEscutaDeACKS();

        while (baseDaJanela < mensagens.size()) {
            janelaLock.lock();
            try {
                if (proximoNumeroDeSequencia < baseDaJanela + TAMANHO_JANELA &&
                    proximoNumeroDeSequencia < mensagens.size()) {
                    enviarNovoQuadro(mensagens.get(proximoNumeroDeSequencia));
                }
            } finally {
                janelaLock.unlock();
            }

//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                System.err.println("Thread principal do emissor foi interrompida.");
//            }
        }

        if (houveFalha) {
            System.out.println("\n\nAlguma mensagem FALHOU após o número máximo de tentativas. Processo encerrado.\n");
        } else {
            System.out.println("\n\nTodas as mensagens foram enviadas e confirmadas. Processo encerrado.\n");
        }
    }

    private void enviarNovoQuadro(String dados) {
        byte seqNum = (byte) (proximoNumeroDeSequencia % NUM_MAX_SEQ);  // ciclico
        Quadro quadro = new Quadro(ENDERECO_EMISSOR, ENDERECO_RECEPTOR, seqNum, Quadro.TIPO_DADOS, dados.getBytes());

        tentativasPorSeqNum[seqNum] = 0;

        janelaDeEnvio.add(quadro);
        System.out.println("EMISSOR: Enviando quadro com SeqNum: " + seqNum);
        enviarParaRede(quadro);
        iniciarTimer(seqNum);
        proximoNumeroDeSequencia++;
    }

    // Em Emissor.java
    private void iniciarEscutaDeACKS() {
        Thread ackListenerThread = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    byte[] buffer = new byte[TAMANHO_BUFFER_ACK];
                    DatagramPacket pacoteRecebido = new DatagramPacket(buffer, buffer.length);
                    socket.receive(pacoteRecebido);

                    // ---- INÍCIO DA CORREÇÃO ----
                    // Crie um novo array com o tamanho exato dos dados do ACK recebido
                    byte[] dadosRecebidos = new byte[pacoteRecebido.getLength()];
                    System.arraycopy(pacoteRecebido.getData(), pacoteRecebido.getOffset(), dadosRecebidos, 0, pacoteRecebido.getLength());

                    // Agora reconstrua o quadro de ACK usando o array de tamanho correto
                    Quadro quadroAck = Quadro.reconstruirQuadro(dadosRecebidos);
                    // ---- FIM DA CORREÇÃO ----

                    if (quadroAck != null && quadroAck.getTipo() == Quadro.TIPO_CONFIRMACAO) {
                        processarACK(quadroAck.getNumeroSequencia());
                    } else if (quadroAck == null) {
                        System.err.println("EMISSOR: Pacote de ACK recebido corrompido ou mal formado. Descartando.");
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

    private void processarACK(int ackNum) {
        janelaLock.lock();
        try {
            System.out.println("\nEMISSOR: Recebeu ACK " + ackNum + ". Base atual: " + (baseDaJanela % NUM_MAX_SEQ));

            if (verificarACKEstaDentroDaJanela(ackNum)) {
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

    private boolean verificarACKEstaDentroDaJanela(int ackNum) {
        int baseSeq = baseDaJanela % NUM_MAX_SEQ;
        int proximoSeq = proximoNumeroDeSequencia % NUM_MAX_SEQ;

        if (baseSeq < proximoSeq) {
            return ackNum > baseSeq && ackNum <= proximoSeq;
        } else {
            return ackNum > baseSeq || ackNum <= proximoSeq;
        }
    }

    private void lidarComTimeOut() {
        janelaLock.lock();
        try {
            System.err.println("\nTIMEOUT! Reenviando toda a janela a partir da base: " + (baseDaJanela % NUM_MAX_SEQ) + "\n");
            for (int i = 0; i < janelaDeEnvio.size(); i++) {
                Quadro quadroParaReenviar = janelaDeEnvio.get(i);
                int seqNumNoQuadro = (baseDaJanela + i) % NUM_MAX_SEQ;

                tentativasPorSeqNum[seqNumNoQuadro]++;
                if (tentativasPorSeqNum[seqNumNoQuadro] > MAX_TENTATIVAS) {
                    System.err.println("EMISSOR: Número máximo de tentativas atingido para SeqNum " + seqNumNoQuadro + ". Desistindo do envio.");
                    pararTimer(seqNumNoQuadro);
                    tentativasPorSeqNum[seqNumNoQuadro] = 0;
                    if (!janelaDeEnvio.isEmpty()) {
                        janelaDeEnvio.remove(i);
                        i--;
                    }
                    baseDaJanela++;
                    houveFalha = true;
                    continue;
                }

                System.err.println("EMISSOR (REENVIO): Enviando quadro com SeqNum: " + seqNumNoQuadro + " (Tentativa " + tentativasPorSeqNum[seqNumNoQuadro] + ")");
                enviarParaRede(quadroParaReenviar);
                iniciarTimer(seqNumNoQuadro);
            }
        } finally {
            janelaLock.unlock();
        }
    }

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
        pararTimer(seqNum);
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
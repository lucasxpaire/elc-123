package model;

public class IniciaReceptor {
    public static void main(String[] args) throws Exception {
        int porta = 5000;
        Receptor receptor = new Receptor();
        receptor.iniciar(porta); // Deve ficar ouvindo indefinidamente
    }
}
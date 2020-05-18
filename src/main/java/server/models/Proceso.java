package server.models;


public class Proceso {
    public int numero;
    public String ip = "";

    @Override
    public String toString() {
        return "Numero: " + numero + " IP: " + ip;
    }
}

package server;
    /* Esta no creo que la use al final*/

public class Estado {

    private static Estado instancia = new Estado();
    private String estado = "libre";
    private int proceso;

    public static Estado getInstancia() {
        return instancia;
    }

    public String pedirEstado() {

        synchronized(estado) {
            try {
                estado.wait();
            } catch (InterruptedException e1) {

                e1.printStackTrace();
            }
        }
        return estado;
    }

}

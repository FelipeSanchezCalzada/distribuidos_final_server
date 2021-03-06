package server;


import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Random;

public class CriticalSection extends Thread {

    final float rango_tarea_min =  0.3f;
    final float rango_tarea_max = 0.5f;
    final float rango_tarea_sc_min =  0.1f;
    final float rango_tarea_sc_max = 0.3f;
    long clock_i;
    long clock_t;
    String ip_propia;
    int num_proceso;

    public CriticalSection(int num, String ip) {
        this.num_proceso = num;
        this.ip_propia = ip;
    }

    public void run() {
        FileWriter fw = null;
        PrintWriter bw = null;

        try {
            fw = new FileWriter("logs.txt");
            bw = new PrintWriter(fw);
            //bw.write("Comienzo del proceso");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        clock_i = 0;
        URI uri = UriBuilder.fromUri("http://" + ip_propia + "/RicartAgrawalaServer").build();
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(uri);
        System.out.println("En sc, mi ip es: " + ip_propia);

        for (int i = 0; i < 100; i++) { // 100 ejecuciones
            System.out.println("iteracion:"+i);
            //difusión de la petición


            try { //simulacion calculos fuera de la seccion critica
                long tiempo_tarea = (long) (1000 * ((new Random().nextFloat() * (this.rango_tarea_max - this.rango_tarea_min)) + this.rango_tarea_min));
                Thread.sleep(tiempo_tarea);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String res = target.path("difusion").request(MediaType.APPLICATION_JSON).get(String.class);
            // Aqui debería esperar el proceso a la respuesta de que tiene la entrada libre
            System.out.println("La respuesta del sistema es: " + res);
            //aceptado por todos


            //entrada seccion critica
            String entrada = String.format("P%d E %s", this.num_proceso + 1, System.currentTimeMillis());
            System.out.println(entrada);
            bw.println(entrada);

            try { //Simulacion calculos seccion critica
                long tiempo_tarea = (long) (1000 * ((new Random().nextFloat() * (this.rango_tarea_sc_max - this.rango_tarea_sc_min)) + this.rango_tarea_sc_min));
                Thread.sleep(tiempo_tarea);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //salida seccion critica


            String salida = String.format("P%d S %s", this.num_proceso + 1, System.currentTimeMillis());
            System.out.println(salida);
            bw.println(salida);


            //mensaje de salida para poder responder a las peticiones
            res = target.path("liberado").request(MediaType.APPLICATION_JSON).post(null, String.class);


            System.out.println("==============================================================");
        }
        try {
            bw.close();
            fw.close();
        } catch (IOException ex) {
            System.err.format("IOException: %s%n", ex);
        }
        if(this.num_proceso == 0){
            String res = target.path("finalizar").request(MediaType.APPLICATION_JSON).post(null,String.class);
        }

    }


}

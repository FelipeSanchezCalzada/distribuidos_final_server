package server;



import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

public class criticalSection extends Thread{

    static final int LIBERADA = 0;
    static final int BUSCADA = 1;
    static final int TOMADA = 2;
    static int estado;
    static long clock_i;
    static long clock_t;
    static String ip_propia;
    static int num_proceso;

    public criticalSection(int num, String ip) {
        this.num_proceso = num;
        this.ip_propia = ip;
    }

    public void run(){
        FileWriter fw = null;
        PrintWriter bw = null;

        try {
            fw = new FileWriter("logs.txt");
            bw = new PrintWriter(fw);
            //bw.write("Comienzo del proceso");
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*try {
            Thread.sleep(5000); //esperamos 5 segundos para que se de tiempo a iniciar todos
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        estado = LIBERADA;
        clock_i = 0;
        URI uri = UriBuilder.fromUri("http://"+ip_propia+"/RicartAgrawalaServer").build();
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(uri);
        System.out.println("en sc, mi ip es:"+ip_propia);

        for(int i=0;i<10;i++) { // 100 ejecuciones

            //difusión de la petición
            estado = BUSCADA;

            String res = target.path("difusion").request(MediaType.APPLICATION_JSON).get(String.class);
            // Aqui debería esperar el proceso a la respuesta de que tiene la entrada libre
            System.out.println("La respuesta del sistema es: " + res);
            //aceptado por todos
            estado = TOMADA;
            //entrada seccion critica
            //TODO escribir log de entrada con el tiempo
            String entrada = String.format("P%d E %s\n",num_proceso,System.currentTimeMillis());
            System.out.printf(entrada);
            bw.write(entrada);


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //salida seccion critica
            estado = LIBERADA;
            //TODO escribir log de salida con el tiempo

            String salida = String.format("P%d S %s\n",num_proceso,System.currentTimeMillis());
            System.out.printf(salida);

                bw.write(salida);


            //mensaje de salida para poder responder a las peticiones
            res = target.path("liberado").request(MediaType.APPLICATION_JSON).post(null, String.class);


            System.out.println("==============================================================");
        }
        try {
                bw.close();
            if (fw != null) {
                fw.close();
            }
        } catch (IOException ex) {
            System.err.format("IOException: %s%n", ex);
        }

    }




}

package server;

import org.glassfish.jersey.client.ClientProperties;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

public class Difusion {

    String ips_procesos;
    private final String delim = "-";
    int procesos=1;
    int C_lamport=0;





    public Difusion(String proc, int C) {
        this.ips_procesos = proc;
        this.C_lamport = C;
    }


    public int check()  {

        /* Ver si es necesario ponerlo sin timeouts en las difusiones,
        la idea es que se quede esperando cada proceso a que le respondan que está libre sin recbir nada antes
        client.property(ClientProperties.CONNECT_TIMEOUT, 0);
        client.property(ClientProperties.READ_TIMEOUT,    0);
        */
        String[] ips = ips_procesos.split(delim);
        procesos = ips.length;

        //TODO multidifusion de las peticiones
        CountDownLatch cdl = new CountDownLatch(procesos); // Esto en teoría espera a que n procesos llames al countdown cuando lo hayan recibido.
        System.out.println("Esperar a:"+procesos);

        for(String ip : ips){
            System.out.println("en difusion a por la ip"+ ip);
            URI uri = UriBuilder.fromUri("http://"+ip+"/RicartAgrawalaServer").build();
            new Thread(new Peticion(uri,cdl,C_lamport)).start();
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return 0;
    }




}

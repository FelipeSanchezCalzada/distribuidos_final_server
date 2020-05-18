package server;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

public class Peticion implements Runnable {

    URI uri;
    Client client = ClientBuilder.newClient();
    WebTarget target;
    CountDownLatch cdl;
    int C_lamport;
    int id;

    public Peticion(URI uri, CountDownLatch c, int reloj, int id) {
        this.uri = uri;
        this.cdl = c;
        this.C_lamport = reloj;
        this.id = id;
    }

    @Override
        public void run() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(uri);
        String res = target.path("peticion").queryParam("reloj",C_lamport).queryParam("id", this.id + "" ).request(MediaType.APPLICATION_JSON).get(String.class);
        System.out.println("La respuesta a la peticion es: " + res);
        cdl.countDown();
        }
}

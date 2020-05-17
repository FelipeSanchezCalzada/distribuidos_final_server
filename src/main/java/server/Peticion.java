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
    public Peticion(URI uri, CountDownLatch c, int reloj) {
        this.uri = uri;
        this.cdl = c;
        this.C_lamport = reloj;
    }

    @Override
        public void run() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(uri);
        String res = target.path("peticion").queryParam("reloj",C_lamport).request(MediaType.APPLICATION_JSON).get(String.class);
        System.out.println("La respuesta a la difusion es: " + res);
        cdl.countDown();
        }
}

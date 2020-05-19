package server;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.models.Proceso;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;


@Path("RicartAgrawalaServer")
@Singleton
public class RicartAgrawalaServer {


    final int LIBERADA = 0;
    final int BUSCADA = 1;
    final int TOMADA = 2;
    private int estado = 0;
    private final long min_t = 1000;
    private final long max_t = 3000;
    private final static Object seccion = new Object();
    ArrayList<Proceso> procesos = new ArrayList<>();// Array de todos los procesos, el primer elemento es el actual
    int C_lamport = 0;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("tiempo")
    public String tiempo() {
        String tiempos = "";
        tiempos = tiempos.concat(String.valueOf(System.currentTimeMillis()));
        return tiempos;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("difusion")
    public Response difusion() {

        estado = BUSCADA;
        ArrayList<Proceso> array_para_difusion = (ArrayList<Proceso>) procesos.clone();
        array_para_difusion.remove(0);
        this.multidifusion(array_para_difusion, C_lamport);

        //hacer la difusion a todas las otras maquinas
        estado = TOMADA;
        C_lamport++;
        return Response.status(Response.Status.OK).entity("Difusion realizada").build();
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("peticion")
    public Response peticion(@QueryParam("reloj") String reloj, @QueryParam("id") String id) {
        // Estado state = Estado.getInstancia();
        System.out.println("en peticion, reloj:"+reloj+" id: "+ id);
        int id_proceso_remoto = Integer.parseInt(id);
        int C_peticion = Integer.parseInt(reloj);
        int Ti = C_lamport;

        C_lamport = (C_lamport > C_peticion) ? (C_lamport + 1) : (C_peticion + 1); //actualizar el valor del reloj

        System.out.println("llamada a petición");



            if (estado == LIBERADA) {
                return Response.status(Response.Status.OK).entity("Acceso concedido (la seccion está libre)").build();
            } else if (estado == BUSCADA) {
                if (C_peticion < Ti) {
                    return Response.status(Response.Status.OK).entity("Acceso concedido (el reloj es menor)").build();
                } else if (C_peticion == Ti) {
                    if (id_proceso_remoto < this.procesos.get(0).numero) {
                        return Response.status(Response.Status.OK).entity("Acceso concedido (el reloj es igual pero el identificador es menor)").build();
                    }
                }
            }
            //si no se le ha concedido acceso debe esperar
            synchronized (seccion) {
                System.out.println("id: " + id_proceso_remoto +  " Esperar por permiso : " + System.currentTimeMillis());
                try {
                    seccion.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error por espera interrumpido").build();
                }
                System.out.println("id: " + id_proceso_remoto + "Permiso concedido : " + System.currentTimeMillis());
                return Response.status(Response.Status.OK).entity("Acceso concedido (la seccion acaba de ser liberada)").build();

            }


    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("liberado")
    public Response liberado() {
        estado = LIBERADA;
        synchronized (seccion) {
            seccion.notifyAll();
        }
        return Response.status(Response.Status.OK).entity("Sección liberada").build();

    }


    /*
     * Inicia el programa el grueso del programa en si mismo.
     * Comienzan a simularse los cálculos, etc.
     *
     * */

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init")
    public Response init(String body) {
        System.out.println(body);
        ObjectMapper mapper = new ObjectMapper();
        Proceso[] array_procesos;
        try {
            array_procesos = mapper.readValue(body, Proceso[].class);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).entity("No se especificaron los procesos en el formato correcto").build();
        }
        if (array_procesos.length == 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Lista de procesos vacía").build();
        }
        this.procesos = new ArrayList<>(Arrays.asList(array_procesos));

        /*
        for (Proceso p: this.procesos){
            System.out.println(p);
        }*/

        CriticalSection cs = new CriticalSection(array_procesos[0].numero, array_procesos[0].ip);
        cs.start();


        System.out.println("Inicialización correcta");
        return Response.status(Response.Status.OK).entity("Corriendo con éxito").build();
    }

    /*
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("iniciarNTP")
    public String iniciarNTP(){
        System.out.println("en IniciarNTP");
        long offset;
        long delay;
        FileWriter fw;
        PrintWriter bw = null;
        try {
            fw = new FileWriter("ntp.txt");
            bw = new PrintWriter(fw);
            bw.write("NTP inicial");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] ips = ips_procesos.split(delim);
        for(String ip : ips){

            offset = Long.MAX_VALUE;
            delay = Long.MAX_VALUE;
            URI uri = UriBuilder.fromUri("http://"+ip+"/NTPserver").build();
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(uri);
            for (int i = 0; i < 10 ; i++) {
                Long t0 = System.currentTimeMillis();
                String res = target.path("tiempo").request(MediaType.APPLICATION_JSON).get(String.class);
                Long t3 = System.currentTimeMillis();
                String[] parts = res.split(delim);
                long t1 = Long.parseLong(parts[0]);
                long t2 = Long.parseLong(parts[1]);
                long t_offset = ( (t1-t0) + (t2-t3) ) / 2;
                long t_delay = (t1-t0) + (t3-t2);

                if(delay > t_delay) {
                    delay = t_delay;
                    offset = t_offset;
                    System.out.println("nuevo delay");
                }
            }
            System.out.println("a escribir");
            bw.write(ip+delim+delay+offset+"\n");

        }


        return "NTP inicial realizado";
    }*/

    /*
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("finalizar")
    public String finalizar(){
        if(num_proceso!=1){
            return "no realizado";
        }
        long offset;
        long delay;
        FileWriter fw;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter("ntp.txt",true);
            bw = new BufferedWriter(fw);
            bw.write("Al finalizar");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] ips = ips_procesos.split(delim);
        for(String ip : ips){

            offset = Long.MAX_VALUE;
            delay = Long.MAX_VALUE;
            URI uri = UriBuilder.fromUri("http://"+ip+"/NTPserver").build();
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(uri);
            for (int i = 0; i < 10 ; i++) {
                Long t0 = System.currentTimeMillis();
                String res = target.path("tiempo").request(MediaType.APPLICATION_JSON).get(String.class);
                Long t3 = System.currentTimeMillis();
                String[] parts = res.split(delim);
                Long t1 = Long.parseLong(parts[0]);
                Long t2 = Long.parseLong(parts[1]);
                Long t_offset = ( (t1-t0) + (t2-t3) ) / 2;
                Long t_delay = (t1-t0) + (t3-t2);

                if(delay > t_delay) {
                    delay = t_delay;
                    offset = t_offset;
                }
            }
            try {
                bw.write(ip+delim+delay+offset+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return "Final realizado";
    }*/


    private int multidifusion(ArrayList<Proceso> lprocesos, int C_lamport) {

        /* Ver si es necesario ponerlo sin timeouts en las difusiones,
        la idea es que se quede esperando cada proceso a que le respondan que está libre sin recbir nada antes
        client.property(ClientProperties.CONNECT_TIMEOUT, 0);
        client.property(ClientProperties.READ_TIMEOUT,    0);
        */

        //TODO multidifusion de las peticiones
        CountDownLatch cdl = new CountDownLatch(lprocesos.size()); // Esto en teoría espera a que n procesos llamen al countdown cuando lo hayan recibido.
        System.out.println("Esperar a:" + lprocesos.size());


        for (Proceso proceso : lprocesos) {
            System.out.println("En difusion a por la ip" + proceso.ip);
            URI uri = UriBuilder.fromUri("http://" + proceso.ip + "/RicartAgrawalaServer").build();
            new Thread(new Peticion(uri, cdl, C_lamport, procesos.get(0).numero)).start();
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }
}

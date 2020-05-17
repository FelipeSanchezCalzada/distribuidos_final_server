package server;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;


@Path("RicartAgrawalaServer")
@Singleton
public class RicartAgrawalaServer {


    final int LIBERADA = 0;
    final int BUSCADA = 1;
    final int TOMADA = 2;
    private int estado = 0;
    private final long min_t = 1000;
    private final long max_t = 3000;
    private final String delim = "-";
    private final static Object seccion = new Object();
    //Difusion dif = new Difusion();
    String ips_procesos = ""; // String con las ips del resto de procesos (sin la ip propia)
    String ip_propia;
    int num_proceso;
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
    public String difusion() {

        estado = BUSCADA;
        Difusion dif = new Difusion(ips_procesos,C_lamport);
        dif.check();

        //hacer la difusion a todas las otras maquinas
        estado = TOMADA;
        C_lamport++;
        return "exito";
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("peticion")
    public String peticion(@QueryParam("reloj") String reloj) {
        // Estado state = Estado.getInstancia();
        int C_peticion = Integer.parseInt(reloj);
        int Ti = C_lamport;

        C_lamport = (C_lamport > C_peticion) ? (C_lamport + 1) : (C_peticion + 1); //actualizar el valor del reloj

        System.out.println("llamada a petición");
        while (true) {
            if (estado == LIBERADA) {

                return "concedido";
            } else if (estado == BUSCADA) {
                    if(C_peticion<Ti){
                        return "concedido por ser C menor";
                    }

            } //si no se le ha concedido acceso debe esperar
            synchronized (seccion) {
                System.out.println("Esperar por permiso");
                        try {
                            seccion.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                }
            }

    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("liberado")
    public Response liberado(){
            estado = LIBERADA;
            synchronized (seccion) {
                seccion.notifyAll();
            }
            return  new Response("Seccion liberada");

    }



    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init")
    public Response init(@QueryParam("procesos") String procesos) {

        System.out.println(procesos);
        String[] parts = procesos.split(delim);
        num_proceso = Integer.parseInt(parts[0]);
        ip_propia = parts[1];
        System.out.println("Mi ip propia es: "+ip_propia);
        System.out.println("Y el resto de ips:");
       /* for( String part : parts){
            if(!part.equals(ip_propia)||!part.equals(String.valueOf(num_proceso))){
                System.out.println(part);
               ips_procesos = ips_procesos.concat(part);
            }
        }*/
       for(int i=2;i<parts.length;i++){
           if(!parts[i].equals(ip_propia)) {
               ips_procesos = ips_procesos.concat(parts[i]);
               ips_procesos = ips_procesos.concat(delim);
           }
       }

        criticalSection cs = new criticalSection(num_proceso, ip_propia);
        cs.start();
        System.out.println("Después de inicialization");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        return new Response("exito");
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("iniciarNTP")
    public String iniciarNTP(@QueryParam("procesos") String procesos){
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
    }

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
                long t1 = Long.parseLong(parts[0]);
                long t2 = Long.parseLong(parts[1]);
                long t_offset = ( (t1-t0) + (t2-t3) ) / 2;
                long t_delay = (t1-t0) + (t3-t2);

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
    }




}

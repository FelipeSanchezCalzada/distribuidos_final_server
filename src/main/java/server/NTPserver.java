package server;


import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("NTPserver")
@Singleton
public class NTPserver {

    private static final long min_t = 1000;
    private static final long max_t = 2000;
    private static final String delim = "-";
    private static boolean first = false;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("tiempo")
    public String tiempo() {

        String tiempos = "";
          tiempos = tiempos.concat(String.valueOf(System.currentTimeMillis()));
          tiempos = tiempos.concat(delim);

        try {
            Thread.sleep((long) (Math.random() * (max_t - min_t)) + min_t);
        } catch (InterruptedException e) {
            e.printStackTrace();

        }

        tiempos = tiempos.concat(String.valueOf(System.currentTimeMillis()));
        System.out.println(tiempos);
        return tiempos;
    }
}

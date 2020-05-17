package server;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
public class Response {
    @XmlElement
    String response;
    public Response(){}
    public Response( String text){
        this.response = text;
    }
}

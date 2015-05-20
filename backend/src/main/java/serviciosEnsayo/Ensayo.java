/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serviciosEnsayo;

import javax.jms.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
/**
 *
 * @author Jonathan
 */
@Path("/Ensayo")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Ensayo {
    
    public ConnectionFactory cf;
    public Connection c;
    public Session s;
    public Destination d;
    public MessageProducer mp;
    /*
    private final static String JNDI_FACTORY ="org.jboss.naming.remote.client.InitialContextFactory";
    private final static String JMS_FACTORY = "jms/RemoteConnectionFactory";
    private final static String QUEUE = "jms/queue/test";
    private final static String jbossUrl = "remote://localhost:4447";
 
    private static InitialContext getInitialContext() throws NamingException {
    Hashtable env = new Hashtable();
    env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
    env.put(Context.PROVIDER_URL, jbossUrl);
    env.put(Context.SECURITY_PRINCIPAL, "jms");
    env.put(Context.SECURITY_CREDENTIALS, "jboss1");
    return new InitialContext(env);
    } */
    
    public void iniciar() throws NamingException, JMSException {
        InitialContext init = new InitialContext();
        cf = (ConnectionFactory) init.lookup("RemoteConnectionFactory");
        d = (Destination) init.lookup("queue/PlayQueue");
        c = (Connection) cf.createConnection("guest123", "guest");
        c.start();
        s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
        mp = s.createProducer(d);
    }
    
     private void send(String string) throws JMSException {
        TextMessage tm = s.createTextMessage(string);
        mp.send(tm);
        }

        private void close() throws JMSException {
        this.c.close();
        }
    
    @GET
    public Integer getTodosLosMuebles() {
        return 1;

    }

    
    
//    @POST
//    @Path("/metodo")
//    public String metodo(List lista) {
//
//        LinkedHashMap l1 = (LinkedHashMap) lista.get(0);
//
//        System.out.println(l1.toString());
//        LinkedHashMap l2 = (LinkedHashMap) lista.get(1);
//
//        System.out.println(l2.toString());
//
//        Clase1 c1 = new Clase1();
//        c1.setNombre(l1.get("nombre").toString());
//        c1.setNumero((int) l1.get("numero"));
//
//        Clase2 c2 = new Clase2();
//        c2.setPalabra(l2.get("palabra").toString());
//        c2.setNum((int) l2.get("num"));
//        return c1.toString() + " - " + c2.toString();
//    }
//
//    @POST
//    @Path("/metodo2")
//    public String metodo2(Clase1 c1) {
//
//        System.out.println(c1.toString());
//        return c1.getNombre();
//    }

    @GET
    @Path("/metodo3")
    public String metodo3() {

        try {
             iniciar();
             send("Ola ca estou eu!");
             s.close();
             return "Bien";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Mal";
        }
        
    }
}

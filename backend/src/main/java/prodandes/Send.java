package prodandes;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Send {

    private ConnectionFactory cf;
    private Connection c;
    private Session s;
    private Destination d;
    private MessageProducer mp;

    public Send() throws NamingException, JMSException {
        InitialContext init = new InitialContext();
        this.cf = (ConnectionFactory) init.lookup("RemoteConnectionFactory");
        this.d = (Destination) init.lookup("queue/PlayQueue");
        this.c = (Connection) this.cf.createConnection("joao", "pedro");
        ((javax.jms.Connection) this.c).start();
        this.s = ((javax.jms.Connection) this.c).createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.mp = this.s.createProducer(this.d);
    }

    public void enviar(String string) throws JMSException {
        TextMessage tm = this.s.createTextMessage(string);
        this.mp.send(tm);
    }

    public void close() throws JMSException {
        this.c.close();
    }
// public static void main(String[] args){
// try {
// Send s = new Send();
// s.enviar("esto es una prueba");
// s.close();
// } catch (Exception e) {
// e.printStackTrace();
// }
//
// }
}

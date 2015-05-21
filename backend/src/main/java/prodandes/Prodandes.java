/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package prodandes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import javax.ejb.BeforeCompletion;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

/**
 *
 * @author Jonathan
 */
@Path("/Servicios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
/**
 *
 * @author jf.molano1587
 */
public class Prodandes implements MessageListener {

    public Connection con;

    private ConnectionFactory cf;
    private javax.jms.Connection c;
    private Session s;
    private Destination d;
    private MessageConsumer mc;
    public static ArrayList<String> buzon;

    // -------------------------------------------------
    // Requerimientos Funcionales
    // -------------------------------------------------
    @POST
    @Path("/registrarPedido")
    public JSONObject registrarPedido(JSONObject jO) throws Exception {
        try {
            JSONObject jRespuesta = new JSONObject();
            String resp = "";

            abrirConexion();

            String lock = "lock table " + "pedido_producto" + " in exclusive mode";
            Statement stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jO.get("fechaEsperada").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String sFecha = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            System.out.println("Fecha: " + sFecha);

            String nombreProducto = jO.get("nombre").toString();

            Statement stx = con.createStatement();
            ResultSet rsx = stx.executeQuery("select * from PRODUCTO where nombre='" + nombreProducto + "'");
            int cantidad = (int) jO.get("cantidad");
            String sId_cliente = jO.get("id_cliente").toString();

            Calendar c = new GregorianCalendar();
            String fechaSolicitud = c.get(GregorianCalendar.DAY_OF_MONTH) + "-"
                    + (c.get(GregorianCalendar.MONTH) + 1) + "-" + c.get(GregorianCalendar.YEAR);

            String fechaEntrega = c.get(GregorianCalendar.DAY_OF_MONTH) + "-"
                    + (c.get(GregorianCalendar.MONTH) + 2) + "-" + c.get(GregorianCalendar.YEAR);
            if (rsx.next()) {

                int id_cliente = Integer.parseInt(sId_cliente);
                System.out.println("FEcha actual " + fechaSolicitud);
                String sql = "select max (id) as MAXIMO from PEDIDO_PRODUCTO";
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);
                escribirEnLog(sql);
                int id_pedido = 1;
                if (rs.next()) {
                    id_pedido = rs.getInt("MAXIMO") + 1;
                    jRespuesta.put("id_pedido", id_pedido);

                    System.out.println("JSON respuesta " + jRespuesta.toString());
                    //Crear pedido nuevo
                    sql = "INSERT INTO PEDIDO_PRODUCTO (id,FECHA_ESPERADA_ENTREGA,Estado,cantidad_producto"
                            + ",id_cliente,fecha_solicitud) VALUES (" + id_pedido + ",TO_DATE"
                            + "('" + sFecha + "','DD-MM-YYYY'),'Espera'," + cantidad + ","
                            + id_cliente + " ,TO_DATE('" + fechaSolicitud + "','DD-MM-YYYY'))";

                    System.out.println("----------------------Query-----------------------");
                    System.out.println(sql);
                    Statement st2 = con.createStatement();

                    st2.executeUpdate(sql);

                    escribirEnLog(sql);
                    st2.close();
                }
                st.close();

                int productosReservados = reservarProductoBodega(nombreProducto, cantidad, id_pedido);

                System.out.println("Productos reservados " + productosReservados);
                if (productosReservados == cantidad) {

                    //Modificar fecha entrega
                    Statement st3 = con.createStatement();
                    sql = "update PEDIDO_PRODUCTO set FECHA_ENTREGA=TO_DATE('" + sFecha + "','DD-MM-YYYY'),"
                            + "ESTADO='En Bodega'"
                            + "where id=" + id_pedido;
                    System.out.println("------------------QUERY----------------------------");
                    System.out.println(sql);
                    st3.executeUpdate(sql);

                    escribirEnLog(sql);
                    st3.close();
                    resp = "En Bodega";
                } else {

                    // Verificar que la cantidad disminuye dependiendo de cuantos productos ya están en bodega
                    cantidad = cantidad - productosReservados;

                    //Verificar que haya estación de produccion disponible
                    sql = "select * from ESTACION where ESTADO='Disponible' AND CAPACIDAD > " + cantidad;
                    System.out.println("------------------QUERY----------------------------");
                    System.out.println(sql);

                    Statement st6 = con.createStatement();
                    rs = st6.executeQuery(sql);

                    boolean hayEstaciones = false;

                    if (rs.next()) {

                        int codigo = rs.getInt("CODIGO");

                        sql = "update ESTACION set ESTADO='Reservado',ID_PEDIDO=" + id_pedido
                                + "WHERE CODIGO=" + codigo;
                        Statement st7 = con.createStatement();
                        st7.executeUpdate(sql);
                        hayEstaciones = true;
                        st7.close();
                    }

                    st6.close();

                    if (hayEstaciones) {
                        //Reservar recursos(materias primas) o pedir suministros
                        int numProductosPotencial = Integer.MAX_VALUE;

                        System.out.println("Nombre producto: " + nombreProducto);
                        //Averiguar Componentes en bodega
                        sql = "select * from COMPONENTES_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                        Statement st3 = con.createStatement();
                        rs = st3.executeQuery(sql);

                        while (rs.next()) {

                            String id_componente = rs.getString("id_componente");
                            int cantidad_unidades = rs.getInt("cantidad_unidades");

                            int numComponentes = cantidadComponentesBodega(id_componente);
                            if (numComponentes >= cantidad_unidades) {

                                int alcanzanComponentes = numComponentes / cantidad_unidades;
                                numProductosPotencial = Math.min(alcanzanComponentes, numProductosPotencial);
                            }
                        }

                        st3.close();

                        //Averiguar Materias Primas en bodega
                        sql = "select * from MATERIAS_PRIMAS_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                        st3 = con.createStatement();
                        rs = st3.executeQuery(sql);

                        while (rs.next()) {

                            String id_materia = rs.getString("id_materia_prima");
                            int cantidad_unidades = rs.getInt("cantidad_unidades");

                            int numMateriasBodega = cantidadMateriasPrimasBodega(id_materia);
                            if (numMateriasBodega >= cantidad_unidades) {

                                int alcanzanMaterias = numMateriasBodega / cantidad_unidades;
                                numProductosPotencial = Math.min(alcanzanMaterias, numProductosPotencial);
                            }
                        }

                        st3.close();

                        System.out.println("Numero productos se pueden hacer con bodega " + numProductosPotencial);

                        if (numProductosPotencial != Integer.MAX_VALUE
                                && numProductosPotencial >= cantidad) {

                            //Reservar componentes
                            sql = "select * from COMPONENTES_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                            st3 = con.createStatement();
                            rs = st3.executeQuery(sql);

                            while (rs.next()) {

                                String id_componente = rs.getString("id_componente");
                                int cantidad_unidades = rs.getInt("cantidad_unidades");
                                reservarComponenteBodega(id_componente, cantidad * cantidad_unidades, id_pedido);
                            }
                            st3.close();

                            // Reservar materias primas
                            sql = "select * from MATERIAS_PRIMAS_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                            st3 = con.createStatement();
                            rs = st3.executeQuery(sql);

                            while (rs.next()) {

                                String id_materia = rs.getString("id_materia_prima");
                                int cantidad_unidades = rs.getInt("cantidad_unidades");
                                reservarMateriaPrimaBodega(id_materia, cantidad * cantidad_unidades, id_pedido);
                            }

                            st3.close();

                            //Falta fecha esperada
                            crearItemsReservadosPedido(nombreProducto, id_pedido, cantidad);
                            resp = "Espera";
                        } else {
                            //Poner el pedido en estad ESPERA
                            st3 = con.createStatement();
                            sql = "update PEDIDO_PRODUCTO set ESTADO='Espera' where id=" + id_pedido;
                            st3.executeUpdate(sql);
                            st3.close();

                            resp = "Buzon";
                            Send env = new Send();
                            //RF18-fecha-idProducto-cantidad-idCliente
                            String sFechaEnv = (cEsp.get(GregorianCalendar.MONTH) + 1) + "/" + cEsp.get(GregorianCalendar.DAY_OF_MONTH)
                                    + "/" + cEsp.get(GregorianCalendar.YEAR);

                            env.enviar("RF18-" + sFechaEnv + "-" + nombreProducto + "-" + cantidad + "-" + id_cliente);
                            env.close();
                            JSONObject jElm = new JSONObject();
                            jElm.put("id_pedido", id_pedido);
                            cancelarPedido(jElm);
                        }
                    } else {

                        resp = "Buzon";
                        Send env = new Send();
                        String sFechaEnv = (cEsp.get(GregorianCalendar.MONTH) + 1) + "/" + cEsp.get(GregorianCalendar.DAY_OF_MONTH)
                                + "/" + cEsp.get(GregorianCalendar.YEAR);

                        env.enviar("RF18-" + sFechaEnv + "-" + nombreProducto + "-" + cantidad + "-" + id_cliente);
                        env.close();
                        JSONObject jElm = new JSONObject();
                        jElm.put("id_pedido", id_pedido);
                        cancelarPedido(jElm);
                    }
                }
                cerrarConexion();
                //return resp;

                if (resp.equals("Buzon")) {
                    Long milis = System.currentTimeMillis();
                    while (System.currentTimeMillis() - milis < 10000) {
                        for (int i = 0; i < buzon.size(); i++) {
                            if (buzon.get(i).startsWith("RF18R-")) {
                                // RF18R-numeroConf-Estado
                                String[] s = buzon.get(i).split("-");
                                jRespuesta = new JSONObject();
                                jRespuesta.put("id_pedido", s[1]);
                                jRespuesta.put("Respuesta", s[2]);
                                buzon.remove(i);
                                return jRespuesta;
                            }
                        }
                    }
                    jRespuesta.put("Respuesta", "Error en la otra aplicacion");
                    return jRespuesta;
                }

                jRespuesta.put("Respuesta", resp);
                return jRespuesta;
            } else {
                System.out.println("No hay productos así en nuestra base de datos");
                resp = "Buzon";
                Send env = new Send();
                String sFechaEnv = (cEsp.get(GregorianCalendar.MONTH) + 1) + "/" + cEsp.get(GregorianCalendar.DAY_OF_MONTH)
                        + "/" + cEsp.get(GregorianCalendar.YEAR);

                String mensaje = "RF18-" + sFechaEnv + "-" + nombreProducto + "-" + cantidad + "-" + sId_cliente;
                env.enviar(mensaje);
                env.close();
                System.out.println("Mensaje a enviar " + mensaje);

                Long milis = System.currentTimeMillis();
                while (System.currentTimeMillis() - milis < 10000) {
                    for (int i = 0; i < buzon.size(); i++) {
                        System.out.println("Entro a buzon");
                        if (buzon.get(i).startsWith("RF18R-")) {
                            // RF18R-numeroConf-Estado
                            String[] s = buzon.get(i).split("-");
                            jRespuesta = new JSONObject();
                            jRespuesta.put("id_pedido", s[1]);
                            jRespuesta.put("Respuesta", s[2]);
                            buzon.remove(i);
                            return jRespuesta;
                        }
                    }
                }
                jRespuesta.put("Respuesta", resp);
                return jRespuesta;
            }
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            //return "error";
            JSONObject jRespuesta = new JSONObject();
            jRespuesta.put("Respuesta", "error");
            return jRespuesta;
        }
    }

    @POST
    @Path("/cancelarPedido")
    public void cancelarPedido(JSONObject jP) throws Exception {

        try {
            abrirConexion();

            String lock = "lock table " + "pedido_producto" + " in exclusive mode";
            Statement stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();

            int id_pedido = (int) jP.get("id_pedido");
            System.out.println("Id Pedido " + id_pedido);
            String sql = "update ESTACION set ESTADO='Disponible',ID_PEDIDO=null WHERE ID_PEDIDO=" + id_pedido;
            System.out.println("------------------QUERY----------------------------");
            System.out.println(sql);

            Statement st = con.createStatement();
            st.executeUpdate(sql);
            st.close();

            sql = "update MATERIA_PRIMA_ITEM set ESTADO='En Bodega',ID_PEDIDO_PRODUCTO=null WHERE ID_PEDIDO_PRODUCTO=" + id_pedido;
            st = con.createStatement();
            System.out.println("------------------QUERY----------------------------");
            System.out.println(sql);
            st.executeUpdate(sql);
            st.close();

            sql = "update COMPONENTE_ITEM set ESTADO='En Bodega',ID_PEDIDO_PRODUCTO=null WHERE ID_PEDIDO_PRODUCTO=" + id_pedido;
            st = con.createStatement();
            System.out.println("------------------QUERY----------------------------");
            System.out.println(sql);
            st.executeUpdate(sql);
            st.close();

            sql = "update ITEM set ESTADO='En Bodega',ID_PEDIDO=null WHERE ID_PEDIDO=" + id_pedido
                    + " AND ESTADO='Reservado'";
            st = con.createStatement();
            System.out.println("------------------QUERY----------------------------");
            System.out.println(sql);
            st.executeUpdate(sql);
            st.close();

            sql = "delete from ITEM WHERE ID_PEDIDO=" + id_pedido + "AND ESTADO='Pre Produccion'";
            st = con.createStatement();
            System.out.println("------------------QUERY----------------------------");
            System.out.println(sql);
            st.executeUpdate(sql);
            st.close();

            sql = "delete from PEDIDO_PRODUCTO WHERE ID=" + id_pedido;
            st = con.createStatement();
            System.out.println("------------------QUERY----------------------------");
            System.out.println(sql);
            st.executeUpdate(sql);
            st.close();
            cerrarConexion();
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
        }
    }

    @POST
    @Path("/registrarEntregaPedidoProductosCliente")
    /**
     * Registrar que los productos del pedido ya se entregaron
     *
     * @param id_pedido
     */
    public void registrarEntregaPedidoProductosCliente(JSONObject jP) throws Exception {
        try {
            int id_pedido = (int) jP.get("id_pedido");
            abrirConexion();

            String lock = "lock table " + "item" + " in exclusive mode";
            Statement stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();
            lock = "lock table " + "pedido_producto" + " in exclusive mode";
            stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();

            String query = "select cantidad_producto from PEDIDO_PRODUCTO WHERE ID=" + id_pedido;

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);
            int cantidad = -1;
            if (rs.next()) {
                cantidad = rs.getInt("cantidad_producto");
            }
            if (cantidad == -1) {
                throw new Exception("No se encontró el pedido");
            }

            st.close();
            query = "select * from ITEM where ID_PEDIDO=" + id_pedido + " and ESTADO='Reservado'";
            st = con.createStatement();
            rs = st.executeQuery(query);
            int cantidadProductosProducidos = 0;
            while (rs.next()) {

                cantidadProductosProducidos++;
            }
            st.close();

            if (cantidadProductosProducidos < cantidad) {
                rollback();
                throw new Exception("No se han terminado de producir todos los productos");

            } else {
                st = con.createStatement();
                rs = st.executeQuery(query);

                while (rs.next()) {

                    int id = rs.getInt("ID");
                    String sql2 = "update ITEM set ESTADO='Entregado' where ID = " + id;

                    Statement st2 = con.createStatement();
                    st2.executeUpdate(sql2);
                    st2.close();
                }

                st.close();

                query = "UPDATE PEDIDO_PRODUCTO SET ESTADO='Entregado' WHERE ID=" + id_pedido;
                st = con.createStatement();
                st.executeUpdate(query);
                st.close();
            }
            cerrarConexion();
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
        }
    }

    @POST
    @Path("/consultarProductos")
    public JSONArray consultarProductos(JSONObject jP) throws Exception {
        try {
            JSONArray jArray = new JSONArray();
            abrirConexion();

            String criterio = jP.get("Criterio").toString();
            if (criterio.equalsIgnoreCase("Rango")) {

                int rango1 = (int) jP.get("Rango1");
                int rango2 = (int) jP.get("Rango2");

                String sql = "Select * from (Select Producto.Nombre as nombreProducto,count(*) as cantidadInventario "
                        + "from (Item inner join Producto on "
                        + "Producto.nombre=Item.nombre_Producto)"
                        + "WHERE Item.Estado ='En Bodega' GROUP BY Producto.nombre) where cantidadInventario>" + rango1
                        + " AND cantidadInventario<" + rango2;
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);

                while (rs.next()) {

                    String nomProd = rs.getString("nombreProducto");

                    sql = "Select * from ITEM where nombre_producto='" + nomProd + "' AND ESTADO='En Bodega'";

                    Statement st2 = con.createStatement();
                    ResultSet rs2 = st2.executeQuery(sql);

                    while (rs2.next()) {
                        JSONObject jObject = new JSONObject();
                        jObject.put("Id", rs2.getInt("id"));
                        jObject.put("Estado", rs2.getString("ESTADO"));
                        jObject.put("Nombre", rs2.getString("NOMBRE_PRODUCTO"));
                        jObject.put("Etapa", rs2.getInt("ETAPA"));
                        jObject.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                        jArray.add(jObject);
                    }

                    st2.close();
                }
                st.close();
            } else if (criterio.equalsIgnoreCase("Etapa")) {

                int num_etapa = (int) jP.get("Etapa");

                String sql = "select * from Item where etapa=" + num_etapa;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {

                    JSONObject jObject = new JSONObject();
                    jObject.put("Id", rs2.getInt("id"));
                    jObject.put("Estado", rs2.getString("ESTADO"));
                    jObject.put("Nombre", rs2.getString("NOMBRE_PRODUCTO"));
                    jObject.put("Etapa", rs2.getInt("ETAPA"));
                    jObject.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jObject);
                }

                st2.close();

            } else if (criterio.equalsIgnoreCase("Fecha solicitud")) {

                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                java.util.Date date = format.parse(jP.get("fecha_solicitud").toString().substring(0, 10));
                System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
                Calendar cEsp = new GregorianCalendar();
                cEsp.setTime(date);

                String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                        + "-" + cEsp.get(GregorianCalendar.YEAR);

                System.out.println("Fecha " + fechaS);
                String sql = "select * from PEDIDO_PRODUCTO where fecha_solicitud = "
                        + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);

                while (rs.next()) {

                    int id_pedido = rs.getInt("id");
                    sql = "select * from ITEM where ID_PEDIDO =" + id_pedido;
                    Statement st2 = con.createStatement();
                    ResultSet rs2 = st2.executeQuery(sql);

                    while (rs2.next()) {
                        JSONObject jO = new JSONObject();
                        jO.put("Id", rs2.getInt("id"));
                        jO.put("Estado", rs2.getString("ESTADO"));
                        jO.put("Nombre", rs2.getString("NOMBRE_PRODUCTO"));
                        jO.put("Etapa", rs2.getInt("ETAPA"));
                        jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                        jArray.add(jO);
                    }
                    st2.close();
                }

                st.close();
            } else if (criterio.equalsIgnoreCase("Fecha entrega")) {

                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                java.util.Date date = format.parse(jP.get("fecha_entrega").toString().substring(0, 10));
                System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
                Calendar cEsp = new GregorianCalendar();
                cEsp.setTime(date);

                String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                        + "-" + cEsp.get(GregorianCalendar.YEAR);

                String sql = "select * from PEDIDO_PRODUCTO where fecha_entrega = "
                        + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);

                while (rs.next()) {

                    int id_pedido = rs.getInt("id");
                    sql = "select * from ITEM where ID_PEDIDO =" + id_pedido;
                    Statement st2 = con.createStatement();
                    ResultSet rs2 = st2.executeQuery(sql);

                    while (rs2.next()) {
                        JSONObject jO = new JSONObject();
                        jO.put("Id", rs2.getInt("id"));
                        jO.put("Estado", rs2.getString("ESTADO"));
                        jO.put("Nombre", rs2.getString("NOMBRE_PRODUCTO"));
                        jO.put("Etapa", rs2.getInt("ETAPA"));
                        jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                        jArray.add(jO);
                    }
                    st2.close();
                }

                st.close();
            }
            cerrarConexion();
            return jArray;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            return null;
        }
    }

    @POST
    @Path("/consultarMateriasPrimas")
    public JSONArray consultarMateriasPrimas(JSONObject jP) throws Exception {

        System.out.println("LLEGO");
        System.out.println("PArametro " + jP.toString());
        JSONArray jArray = new JSONArray();
        abrirConexion();
        String criterio = jP.get("Criterio").toString();

        System.out.println("Criterio " + criterio);
        if (criterio.equalsIgnoreCase("Rango")) {

            int rango1 = (int) jP.get("Rango1");
            int rango2 = (int) jP.get("Rango2");

            System.out.println("Rango1" + rango1);
            System.out.println("Rango2" + rango2);
            String sql = "Select * from (Select Materia_Prima.Nombre as nombreMateria,count(*) as "
                    + "cantidadInventario "
                    + "from (Materia_Prima_Item inner join Materia_Prima on "
                    + "Materia_Prima.nombre=Materia_Prima_Item.materia)"
                    + "WHERE Materia_Prima_Item.Estado ='En Bodega' "
                    + "GROUP BY Materia_Prima.nombre) where cantidadInventario>" + rango1
                    + " AND cantidadInventario<" + rango2;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                String materia = rs.getString("nombreMateria");

                System.out.println("Materia " + materia);
                sql = "Select * from Materia_Prima_Item where materia='" + materia + "' "
                        + "AND Estado='En Bodega'";

                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("Id", rs2.getInt("id"));
                    jO.put("Estado", rs2.getString("ESTADO"));
                    jO.put("MateriaPrima", rs2.getString("MATERIA"));
                    jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));

                    jArray.add(jO);
                }

                st2.close();
            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Fecha solicitud")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_solicitud").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1) + "-"
                    + cEsp.get(GregorianCalendar.YEAR);

            System.out.println("FEcha pedido " + fechaS);

            String sql = "select * from PEDIDO_MATERIA_PRIMA "
                    + "where FECHA_PEDIDO="
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                System.out.println("id_pedido " + id_pedido);
                sql = "select * from MATERIA_PRIMA_ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("Id", rs2.getInt("id"));
                    jO.put("Estado", rs2.getString("ESTADO"));
                    jO.put("MateriaPrima", rs2.getString("MATERIA"));
                    jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        } else if (criterio.equalsIgnoreCase("Fecha entrega")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_entrega").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            String sql = "select * from PEDIDO_MATERIA_PRIMA where fecha_entrega="
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                sql = "select * from MATERIA_PRIMA_ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("Id", rs2.getInt("id"));
                    jO.put("Estado", rs2.getString("ESTADO"));
                    jO.put("MateriaPrima", rs2.getString("MATERIA"));
                    jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        } else if (criterio.equalsIgnoreCase("Tipo_material")) {

            String tipo = jP.get("Tipo_material").toString();

            System.out.println("TIPO " + tipo);
            String sql = "select * from MATERIA_PRIMA where TIPO = '" + tipo + "'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                String nombreMateria = rs.getString("nombre");
                System.out.println("Nombre materia " + nombreMateria);
                sql = "select * from MATERIA_PRIMA_ITEM where materia = '" + nombreMateria + "'";
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("Id", rs2.getInt("id"));
                    jO.put("Estado", rs2.getString("ESTADO"));
                    jO.put("MateriaPrima", rs2.getString("MATERIA"));
                    jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);

                }
            }
        }
        cerrarConexion();
        return jArray;
    }

    @POST
    @Path("/consultarComponentes")
    public JSONArray consultarComponentes(JSONObject jP) throws Exception {

        JSONArray jArray = new JSONArray();
        abrirConexion();

        String criterio = jP.get("Criterio").toString();
        if (criterio.equalsIgnoreCase("Rango")) {

            int rango1 = (int) jP.get("Rango1");
            int rango2 = (int) jP.get("Rango2");

            String sql = "Select * from (Select Componente.Nombre as nombreComponente,count(*) as "
                    + "cantidadInventario "
                    + "from (Componente_Item inner join Componente on "
                    + "Componente.nombre=Componente_Item.componente)"
                    + "WHERE Componente_Item.Estado ='En Bodega' "
                    + "GROUP BY Componente.nombre) where cantidadInventario>" + rango1
                    + " AND cantidadInventario<" + rango2;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                String componente = rs.getString("nombreComponente");

                sql = "Select * from componente_item where componente='" + componente + "'"
                        + "AND Estado ='En Bodega'";

                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("Id", rs2.getInt("id"));
                    jO.put("Estado", rs2.getString("ESTADO"));
                    jO.put("Componente", rs2.getString("COMPONENTE"));
                    jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));

                    jArray.add(jO);
                }

                st2.close();
            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Fecha solicitud")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_solicitud").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            String sql = "select * from PEDIDO_COMPONENTE where fecha_pedido = "
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                sql = "select * from COMPONENTE_ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("Id", rs2.getInt("id"));
                    jO.put("Estado", rs2.getString("ESTADO"));
                    jO.put("Componente", rs2.getString("COMPONENTE"));
                    jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        } else if (criterio.equalsIgnoreCase("Fecha entrega")) {

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha_entrega").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            String sql = "select * from PEDIDO_componente where fecha_entrega = "
                    + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                int id_pedido = rs.getInt("id");
                sql = "select * from componente_ITEM where ID_PEDIDO =" + id_pedido;
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("Id", rs2.getInt("id"));
                    jO.put("Estado", rs2.getString("ESTADO"));
                    jO.put("Componente", rs2.getString("COMPONENTE"));
                    jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);
                }
                st2.close();
            }

            st.close();
        } else if (criterio.equalsIgnoreCase("Tipo_material")) {

            String tipo = jP.get("Tipo_material").toString();

            String sql = "select * from componente where TIPO = '" + tipo + "'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                String componente = rs.getString("nombre");
                sql = "select * from componente_ITEM where componente = '" + componente + "'";
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                while (rs2.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("Id", rs2.getInt("id"));
                    jO.put("Estado", rs2.getString("ESTADO"));
                    jO.put("Componente", rs2.getString("COMPONENTE"));
                    jO.put("IdPedido", rs2.getInt("ID_PEDIDO"));
                    jArray.add(jO);

                }
            }
        }
        cerrarConexion();
        return jArray;
    }

    // -------------------------------------------------
    // Metodos Adicionales
    // -------------------------------------------------
    public void crearItemsReservadosPedido(String nombreProducto, int id_pedido, int cantidad) throws Exception {
        try {
            for (int i = 0; i < cantidad; i++) {
                String sql = "select max (id) as MAXIMO from ITEM";
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);
                int id_item = -1;
                if (rs.next()) {
                    id_item = rs.getInt("MAXIMO") + 1;

                    //Crear pedido nuevo
                    sql = "INSERT INTO ITEM (id,ESTADO,NOMBRE_PRODUCTO,ID_PEDIDO)"
                            + " VALUES (" + id_item + ",'Pre Produccion','" + nombreProducto + "',"
                            + id_pedido + ")";

                    Statement st2 = con.createStatement();

                    st2.executeUpdate(sql);

                    st2.close();
                }
                st.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
        }
    }

    @POST
    @Path("/cantidadProductoEnBodega")
    public int cantidadProductoEnBodega(String nombre) throws Exception {
        System.out.println("Entrada parámetro cantidadProductoEnBodega");
        System.out.println(nombre);

        String query = "select count(*) as cuenta from ITEM where NOMBRE_PRODUCTO='" + nombre + "' and ESTADO='En Bodega'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        int resp = 0;
        if (rs.next()) {
            resp = rs.getInt("cuenta");
        }
        st.close();

        System.out.println("Return cantidadProductoEnBodega: " + resp);
        return resp;
    }

    @POST
    @Path("/cantidadMateriasPrimasBodega")
    public int cantidadMateriasPrimasBodega(String materia) throws Exception {
        System.out.println("Entrada parámetro cantidadMateriasPrimasBodega");
        System.out.println(materia);

        String query = "select count(*) as cuenta from MATERIA_PRIMA_ITEM "
                + "where MATERIA='" + materia + "' and ESTADO='En Bodega'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        int resp = 0;
        if (rs.next()) {
            resp = rs.getInt("cuenta");
        }
        st.close();

        System.out.println("Return cantidadMateriasPrimasBodega: " + resp);
        return resp;
    }

    @POST
    @Path("/cantidadComponentesBodega")
    public int cantidadComponentesBodega(String componente) throws Exception {
        System.out.println("Entrada parámetro cantidadComponentesBodega");
        System.out.println(componente);

        String query = "select count(*) as cuenta from COMPONENTE_ITEM "
                + "where COMPONENTE='" + componente + "' and ESTADO='En Bodega'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        int resp = 0;
        if (rs.next()) {
            resp = rs.getInt("cuenta");
        }
        st.close();

        System.out.println("Return cantidadComponentesBodega: " + resp);
        return resp;
    }

    public void abrirConexion() throws Exception {

        con = null;
        Class.forName("oracle.jdbc.driver.OracleDriver");
        con = DriverManager.getConnection("jdbc:oracle:thin:@157.253.238.224:1531:prod", "ISIS2304271510", "rproxyquark");
        con.setAutoCommit(true);
    }

    public void cerrarConexion() throws Exception {
        if (con != null) {
            con.commit();
            con.close();
            con = null;
        }
    }

    public void rollback() {
        if (con != null) {
            try {
                con.rollback();
                con.close();
                con = null;
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Se reserva la cantidad de ese producto que está en bodega, y se pasa a
     * estado reservado y se asocia los items con el pedido
     *
     * @param nombreProducto
     * @param cantidad
     * @param id_pedido
     * @throws Exception
     */
    public int reservarProductoBodega(String nombreProducto, int cantidad, int id_pedido) throws Exception {
        try {
            System.out.println("Reservar Productos Bodega " + nombreProducto + "," + cantidad + "," + id_pedido);
            String query = "select * from ITEM where NOMBRE_PRODUCTO='" + nombreProducto + "' and ESTADO='En Bodega'";

            System.out.println("------------------------QUERY-----------------------");
            System.out.println(query);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);

            int i;
            for (i = 0; i < cantidad && rs.next(); i++) {

                int id = rs.getInt("ID");
                String sql2 = "update ITEM set ESTADO='Reservado',ID_PEDIDO=" + id_pedido + " where ID = " + id;
                System.out.println("------------------------QUERY-----------------------");
                System.out.println(sql2);
                Statement st2 = con.createStatement();
                st2.executeUpdate(sql2);
                st2.close();
            }

            st.close();
            return i;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            return 0;
        }
    }

    public int reservarComponenteBodega(String id_componente, int cantidad_unidades, int id_pedido)
            throws Exception {
        try {
            System.out.println("reservarComponenteBodega " + cantidad_unidades);
            String query = "select * from COMPONENTE_ITEM where componente='" + id_componente
                    + "' and ESTADO='En Bodega'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);

            int i;
            for (i = 0; i < cantidad_unidades && rs.next(); i++) {

                int id = rs.getInt("id");
                String sql2 = "update COMPONENTE_ITEM set ESTADO='Reservado',ID_PEDIDO_PRODUCTO=" + id_pedido
                        + " where id =" + id;

                Statement st2 = con.createStatement();
                st2.executeUpdate(sql2);
                st2.close();
            }

            st.close();
            return i;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    public int reservarMateriaPrimaBodega(String id_materia, int cantidad_unidades, int id_pedido)
            throws Exception {
        try {
            System.out.println("reservarComponenteBodega " + cantidad_unidades);

            String query = "select * from MATERIA_PRIMA_ITEM where materia='" + id_materia
                    + "' and ESTADO='En Bodega'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);

            int i;
            for (i = 0; i < cantidad_unidades && rs.next(); i++) {

                int id = rs.getInt("id");
                String sql2 = "update MATERIA_PRIMA_ITEM set ESTADO='Reservado',ID_PEDIDO_PRODUCTO=" + id_pedido
                        + " where id=" + id;

                System.out.println("-------------------------QUERY-------------------------");
                System.out.println(sql2);
                Statement st2 = con.createStatement();
                st2.executeUpdate(sql2);
                st2.close();
            }

            st.close();
            return i;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    //-------------------------------------------------------
    // Métodos para ensayar otros métodos
    //-------------------------------------------------------
    @POST
    @Path("/reservarProducto")
    public void reservarProductoREST(List lista) throws Exception {

        abrirConexion();

        LinkedHashMap lNombreProducto = (LinkedHashMap) lista.get(0);
        LinkedHashMap lCantidad = (LinkedHashMap) lista.get(1);
        LinkedHashMap lIdPedido = (LinkedHashMap) lista.get(2);

        reservarProductoBodega(lNombreProducto.get("nombreProducto").toString(), (int) lCantidad.get("cantidad"),
                (int) lIdPedido.get("id_pedido"));

        cerrarConexion();
    }

    @POST
    @Path("/registrarProveedor")
    public void registrarProveedor(JSONObject proveedor) throws Exception {
        abrirConexion();
        try {
            String lock = "lock table " + "proveedor" + " in exclusive mode";
            Statement stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();
            System.out.println("- - - - - - - - - - - - - - - - - Print Entrada - - - - - - - - - - - - - - - - -");
            System.out.println(proveedor);
            int doc = Integer.parseInt(proveedor.get("documento").toString());
            String nombre = proveedor.get("nombre").toString();
            String ciudad = proveedor.get("ciudad").toString();
            String direccion = proveedor.get("direccion").toString();
            String telefono = proveedor.get("telefono").toString();
            int volumenMax = Integer.parseInt(proveedor.get("volumenMaximo").toString());
            int tiempoResp = Integer.parseInt(proveedor.get("tiempoDeEntrega").toString());
            String representante = proveedor.get("representanteLegal").toString();
//        String sql = "select max (id) as MAXIMO from PROVEEDOR";
//        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
//        System.out.println(sql);
//        Statement st = con.createStatement();
//        ResultSet rs = st.executeQuery(sql);
//        st.close();
//        int id_item = -1;
//        if (rs.next()) {
//            id_item = rs.getInt("MAXIMO") + 1;
//        }
            String sql = "INSERT INTO PROVEEDOR (DOCUMENTO_ID,NOMBRE,CIUDAD,DIRECCION,TELEFONO,VOLUMEN_MAXIMO,TIEMPO_DE_ENTREGA,REPRESENTANTE_LEGAL)" + " VALUES ('" + doc + "','" + nombre + "','" + ciudad + "','" + direccion + "','" + telefono + "'," + volumenMax + "," + tiempoResp + "," + representante + ")";
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(sql);
            Statement st2 = con.createStatement();
            st2.executeUpdate(sql);
            st2.close();
            cerrarConexion();
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    @POST
    @Path("/registrarLlegadaDeMaterial")
    public void registrarLlegadaDeMaterial(JSONObject idPedidoMateriaPrimaP) throws Exception {
        abrirConexion();
        try {
            String lock = "lock table " + "PEDIDO_MATERIA_PRIMA" + " in exclusive mode";
            Statement stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();

            lock = "lock table " + "MATERIA_PRIMA_ITEM" + " in exclusive mode";
            stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();
            int idPedidoMateriaPrima = Integer.parseInt(idPedidoMateriaPrimaP.get("id").toString());
            System.out.println("Entrada parÃ¡metro registrarLlegadaDeMaterial");
            System.out.println(idPedidoMateriaPrima);
            Calendar c = new GregorianCalendar();
            String fecha = c.get(GregorianCalendar.DAY_OF_MONTH) + "-"
                    + (c.get(GregorianCalendar.MONTH) + 1) + "-" + c.get(GregorianCalendar.YEAR);
            String query = "update PEDIDO_MATERIA_PRIMA set FECHA_ENTREGA = TO_DATE ('" + fecha + "','DD-MM-YYYY') WHERE ID = " + idPedidoMateriaPrima;
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query);
            Statement st = con.createStatement();
            st.executeQuery(query);
            st.close();
            query = "update MATERIA_PRIMA_ITEM set ESTADO = 'En Bodega' WHERE ID_PEDIDO =" + idPedidoMateriaPrima;
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query);
            st = con.createStatement();
            st.executeQuery(query);
            st.close();
            cerrarConexion();
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    @POST
    @Path("/registrarLlegadaDeComponentes")
    public void registrarLlegadaDeComponentes(JSONObject idPedidoComponenteP) throws Exception {
        abrirConexion();
        try {
            String lock = "lock table " + "PEDIDO_COMPONENTE" + " in exclusive mode";
            Statement stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();
            lock = "lock table " + "COMPONENTE_ITEM" + " in exclusive mode";
            stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();
            int idPedidoComponente = Integer.parseInt(idPedidoComponenteP.get("id").toString());
            System.out.println("Entrada parÃ¡metro registrarLlegadaDeMaterial");
            System.out.println("Marca 1");
            System.out.println(idPedidoComponente);
            Calendar c = new GregorianCalendar();
            System.out.println("Marca 2");
            String fecha = c.get(GregorianCalendar.DAY_OF_MONTH) + "-"
                    + (c.get(GregorianCalendar.MONTH) + 1) + "-" + c.get(GregorianCalendar.YEAR);
            System.out.println("Marca 3");
            String query = "update PEDIDO_COMPONENTE set FECHA_ENTREGA = TO_DATE ('" + fecha + "','DD-MM-YYYY') WHERE ID = " + idPedidoComponente;
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query);
            Statement st = con.createStatement();
            st.executeQuery(query);
            st.close();
            query = "update COMPONENTE_ITEM set ESTADO = 'En Bodega' WHERE ID_PEDIDO =" + idPedidoComponente;
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query);
            st = con.createStatement();
            st.executeQuery(query);
            st.close();
            cerrarConexion();
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    @POST
    @Path("/registrarEjecucionEtapa")
    public JSONObject registrarEjecucionEtapa(JSONObject num_secuenciaP) throws Exception {
        abrirConexion();
        try {
            String lock = "lock table " + "item" + " in exclusive mode";
            Statement stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();
            lock = "lock table " + "MATERIA_PRIMA_ITEM" + " in exclusive mode";
            stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();
            lock = "lock table " + "ITEM_COMPONENTE_ETAPA" + " in exclusive mode";
            stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();
            lock = "lock table " + "COMPONENTE_ITEM" + " in exclusive mode";
            stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();
            int num_secuencia = Integer.parseInt(num_secuenciaP.get("secuencia").toString());
            //Verificar productos
            System.out.println("Entrada parÃ¡metro registrarEjecucionEtapa");
            System.out.println(num_secuencia);
            int numProductosDisponibles = verificarProductosEstacionAnterior(num_secuencia);
            if (numProductosDisponibles == 0) {
                JSONObject jRespuesta = new JSONObject();
                jRespuesta.put("Respuesta", "Numero de productos no disponibles");
                return jRespuesta;
            }
            //Verificar componentes
            String query = "select * from (select CANTIDAD, (case when cuenta is null then 0 else cuenta end) as CUENTA from (select * from ITEM_COMPONENTE_ETAPA left outer join (select COMPONENTE, count(ID) as cuenta from COMPONENTE_ITEM where COMPONENTE_ITEM.ESTADO = 'En Bodega' group by COMPONENTE) on COMPONENTE = COMPONENTE_NOMBRE) where NUMERO_SECUENCIA =" + num_secuencia + ") where CANTIDAD > cuenta";
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {

                JSONObject jRespuesta = new JSONObject();
                jRespuesta.put("Respuesta", "Cantidad de componentes insuficientes");
                return jRespuesta;
            }
            st.close();
            //Verificar materia
            String query1 = "select * from (select CANTIDAD, (case when cuenta is null then 0 else cuenta end) as CUENTA from (select * from ITEM_MATERIA_PRIMA_ETAPA left outer join (select MATERIA, count(ID) as cuenta from MATERIA_PRIMA_ITEM where MATERIA_PRIMA_ITEM.ESTADO = 'En Bodega' group by MATERIA) on MATERIA = MATERIA_PRIMA_NOMBRE) where NUMERO_SECUENCIA =" + num_secuencia + ") where CANTIDAD > cuenta";
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query1);
            Statement st1 = con.createStatement();
            ResultSet rs1 = st1.executeQuery(query1);
            if (rs1.next()) {

                JSONObject jRespuesta = new JSONObject();
                jRespuesta.put("Respuesta", "Cantidad de materia prima insuficiente");
                return jRespuesta;
            }
            st1.close();
            //Subir productos en etapas
            String query2 = "update item set ETAPA = ETAPA+1 where ETAPA = " + (num_secuencia - 1) + "AND ID = (select min(ID) from ITEM where ETAPA = " + (num_secuencia - 1) + ")";
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query2);
            Statement st2 = con.createStatement();
            st2.executeQuery(query2);
            st2.close();
            //Reducir suministros MATERIA PRIMA
            String query3 = "select * from ITEM_MATERIA_PRIMA_ETAPA where NUMERO_SECUENCIA = " + num_secuencia;
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query3);
            Statement st3 = con.createStatement();
            ResultSet rs3 = st3.executeQuery(query3);
            ArrayList<String> lista1 = new ArrayList();
            ArrayList<Integer> lista2 = new ArrayList();
            while (rs3.next()) {
                lista1.add(rs3.getString("MATERIA_PRIMA_NOMBRE"));
                lista2.add(rs3.getInt("CANTIDAD"));
            }
            st3.close();
            String query4;
            Statement st4;
            ResultSet rs4;
            for (int i = 0; i < lista1.size(); i++) {
                for (int j = 0; j < lista2.get(i); j++) {
                    query4 = "DELETE FROM MATERIA_PRIMA_ITEM where ID = (select min(ID) from MATERIA_PRIMA_ITEM where MATERIA = '" + lista1.get(i) + "')";
                    System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
                    System.out.println(query4);
                    st4 = con.createStatement();
                    rs4 = st4.executeQuery(query4);
                    st4.close();
                }
            }
            //Registrar etapa
            Calendar c = new GregorianCalendar();
            String fecha = c.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (c.get(GregorianCalendar.MONTH) + 1) + "-" + c.get(GregorianCalendar.YEAR) + " " + c.get(GregorianCalendar.HOUR_OF_DAY) + ":" + c.get(GregorianCalendar.MINUTE) + ":" + c.get(GregorianCalendar.SECOND);
            String query20 = "INSERT INTO ETAPA_FECHA (CODIGO_SECUENCIA, FECHA) VALUES (" + num_secuencia + ", TO_DATE('" + fecha + "', 'DD-MM-YYYY HH24:MI:SS'))";
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query20);
            Statement st20 = con.createStatement();
            st20.executeQuery(query20);
            st20.close();
            //Reducir suministros COMPONENTE
            query3 = "select * from ITEM_COMPONENTE_ETAPA where NUMERO_SECUENCIA = " + num_secuencia;
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query3);
            st3 = con.createStatement();
            rs3 = st3.executeQuery(query3);
            lista1 = new ArrayList();
            lista2 = new ArrayList();
            while (rs3.next()) {
                lista1.add(rs3.getString("COMPONENTE_NOMBRE"));
                lista2.add(rs3.getInt("CANTIDAD"));
            }
            st3.close();
            for (int i = 0; i < lista1.size(); i++) {
                for (int j = 0; j < lista2.get(i); j++) {
                    query4 = "DELETE FROM COMPONENTE_ITEM where ID = (select min(ID) from COMPONENTE_ITEM where COMPONENTE = '" + lista1.get(i) + "')";
                    System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
                    System.out.println(query4);
                    st4 = con.createStatement();
                    rs4 = st4.executeQuery(query4);
                    st4.close();
                }
            }
            cerrarConexion();

            JSONObject jRespuesta = new JSONObject();
            jRespuesta.put("Respuesta", "Operacion correcta");
            return jRespuesta;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    @POST
    @Path("/verificarProductosEstacionAnterior")
    public int verificarProductosEstacionAnterior(int numSecuencia) throws Exception {
        try {
            //Dar etapa y producto del num_secuencia
//        String query3 = "select etapa, nombre_producto from ETAPA_DE_PRODUCCION where NUMERO_SECUENCIA = " + numSecuencia;
//        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
//        System.out.println(query3);
//        Statement st3 = con.createStatement();
//        ResultSet rs3 = st3.executeQuery(query3);
//        int resp3 = 0;
//        String resp4 = "";
//        if (rs3.next()) {
//            resp3 = rs3.getInt("etapa");
//            System.out.println("etapa:");
//            System.out.println(resp3);
//            resp4 = rs3.getString("nombre_producto");
//            System.out.println("nombre_producto:");
//            System.out.println(resp4);
//        }
//        int etapa = resp3;
//        int etapaAnterior = etapa - 1;
//        String producto = resp4;
//        st3.close();
//        //Dar id de la etapa anterior
//        String query4 = "select NUMERO_SECUENCIA from ETAPA_DE_PRODUCCION where ETAPA= " + etapaAnterior + " AND " + "NOMBRE_PRODUCTO ='" + producto + "'";
//        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
//        System.out.println(query4);
//        Statement st4 = con.createStatement();
//        ResultSet rs4 = st4.executeQuery(query4);
//        int resp5 = 0;
//        if (rs4.next()) {
//            resp5 = rs4.getInt("NUMERO_SECUENCIA");
//            System.out.println("NUMERO_SECUENCIA:");
//            System.out.println(resp5);
//        }
//        st4.close();
//        int numSecAnterior = resp5;
            //Dar numero de items en etapa anterior
            String query5 = "select count(*) as cuenta from ITEM where ETAPA= " + (numSecuencia - 1);
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query5);
            Statement st5 = con.createStatement();
            ResultSet rs5 = st5.executeQuery(query5);
            int resp6 = 0;
            if (rs5.next()) {
                resp6 = rs5.getInt("cuenta");
                System.out.println("cuenta:");
                System.out.println(resp6);
            }
            st5.close();
            //FIN
            System.out.println("Return verificarProductosEstacionAnterior: " + resp6);
            return resp6;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    // Comparar rangos
    @POST
    @Path("/consultarEtapaProduccionMayorMovimiento")
    public JSONObject consultarEtapaProduccionMayorMovimiento(JSONObject jO) throws Exception {

        abrirConexion();
        try {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date dateIni = format.parse(jO.get("fechaInicio").toString());
            java.util.Date dateFin = format.parse(jO.get("fechaFin").toString());
            Calendar dateIniCalendar = new GregorianCalendar();
            dateIniCalendar.setTime(dateIni);
            Calendar dateFinCalendar = new GregorianCalendar();
            dateFinCalendar.setTime(dateFin);
            String query5 = "SELECT * from ("
                    + "SELECT CODIGO_SECUENCIA, count(FECHA) as cuenta from ("
                    + "select * from ETAPA_FECHA where TO_DATE('" + dateFinCalendar.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (dateFinCalendar.get(GregorianCalendar.MONTH) + 1) + "-" + dateFinCalendar.get(GregorianCalendar.YEAR) + "','DD-MM-YYYY')>ETAPA_FECHA.FECHA AND TO_DATE('" + dateIniCalendar.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (dateIniCalendar.get(GregorianCalendar.MONTH) + 1) + "-" + dateIniCalendar.get(GregorianCalendar.YEAR) + "','DD-MM-YYYY')<ETAPA_FECHA.FECHA)"
                    + "GROUP BY CODIGO_SECUENCIA) where cuenta = ("
                    + "SELECT max (count(FECHA)) from (select * from ETAPA_FECHA where TO_DATE('" + dateFinCalendar.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (dateFinCalendar.get(GregorianCalendar.MONTH) + 1) + "-" + dateFinCalendar.get(GregorianCalendar.YEAR) + "','DD-MM-YYYY')>ETAPA_FECHA.FECHA AND TO_DATE('" + dateIniCalendar.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (dateIniCalendar.get(GregorianCalendar.MONTH) + 1) + "-" + dateIniCalendar.get(GregorianCalendar.YEAR) + "','DD-MM-YYYY')<ETAPA_FECHA.FECHA) "
                    + "GROUP BY CODIGO_SECUENCIA)";
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query5);
            Statement st5 = con.createStatement();
            ResultSet rs5 = st5.executeQuery(query5);
            int numeroMaximo = 0;
            String etapaMaxima = "";
            if (rs5.next()) {
                numeroMaximo = rs5.getInt("CUENTA");
                System.out.println("CUENTA:");
                System.out.println(numeroMaximo);
                etapaMaxima = rs5.getString("CODIGO_SECUENCIA");
                System.out.println("CODIGO_SECUENCIA:");
                System.out.println(etapaMaxima);
            }
            st5.close();
            JSONObject resp = new JSONObject();
            resp.put("CODIGO_SECUENCIA", etapaMaxima);
            resp.put("CUENTA", numeroMaximo);
            cerrarConexion();

            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    // Operario mas activo
    @POST
    @Path("/operarioMasActivo")
    public JSONObject operarioMasActivo(JSONObject jO) throws Exception {

        abrirConexion();
        try {
            int num_secuencia = Integer.parseInt(jO.get("secuencia").toString());

            String query5 = "select * from OPERARIO natural join ("
                    + "select DOCUMENTO_OPERARIO as DOCUMENTO_ID, CUENTA from ("
                    + "select * from (select DOCUMENTO_OPERARIO, count(ID_ETAPA) as cuenta from ETAPAS_OPERARIOS where ID_ETAPA= " + num_secuencia + " group by DOCUMENTO_OPERARIO) "
                    + "where cuenta=("
                    + "SELECT max(count(ID_ETAPA)) as cuenta from ETAPAS_OPERARIOS "
                    + "where ID_ETAPA= " + num_secuencia
                    + " group by DOCUMENTO_OPERARIO)))";
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(query5);
            Statement st5 = con.createStatement();
            ResultSet rs5 = st5.executeQuery(query5);
            JSONObject operario = new JSONObject();
            String documento_id = "";
            String nombre = "";
            String nacionalidad = "";
            String direccion = "";
            String email = "";
            String telefono = "";
            String ciudad = "";
            String departamento = "";
            String codigo_postal = "";
            JSONArray lista = new JSONArray();
            int i = 0;
            while (rs5.next()) {
                i++;
                System.out.println("Numero del ciclo: " + i);
                documento_id = rs5.getString("DOCUMENTO_ID");
                nombre = rs5.getString("NOMBRE");
                nacionalidad = rs5.getString("NACIONALIDAD");
                direccion = rs5.getString("DIRECCION");
                email = rs5.getString("EMAIL");
                telefono = rs5.getString("TELEFONO");
                ciudad = rs5.getString("CIUDAD");
                departamento = rs5.getString("DEPARTAMENTO");
                codigo_postal = rs5.getString("CODIGO_POSTAL");
                operario.put("DOCUMENTO_ID", documento_id);
                operario.put("NOMBRE", nombre);
                operario.put("NACIONALIDAD", nacionalidad);
                operario.put("DIRECCION", direccion);
                operario.put("EMAIL", email);
                operario.put("TELEFONO", telefono);
                operario.put("CIUDAD", ciudad);
                operario.put("DEPARTAMENTO", departamento);
                operario.put("CODIGO_POSTAL", codigo_postal);
                System.out.println("Operario: " + operario);
                lista.add(operario);
            }
            System.out.println("Lista: " + lista);
            st5.close();
            cerrarConexion();
            JSONObject resp = new JSONObject();
            resp.put("operarios", lista.toString());
            System.out.println("Respuesta: " + resp.toString());
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    //---------------------------------------------------------------------------------------------------------------
    //------------------------ Iteracion 3---------------------------------------------------------------------------
    //---------------------------------------------------------------------------------------------------------------
    @POST
    @Path("/consultarPedidos")
    public JSONArray consultarPedidos(JSONObject jP) throws Exception {

        JSONArray jArray = new JSONArray();
        abrirConexion();
        try {
            String criterio = jP.get("Criterio").toString();
            if (criterio.equalsIgnoreCase("Cantidad")) {

                int rango1 = (int) jP.get("Rango1");
                int rango2 = (int) jP.get("Rango2");

                String sql = "select * from PEDIDO_PRODUCTO where CANTIDAD_PRODUCTO >=" + rango1 + " AND CANTIDAD_PRODUCTO <= " + rango2;
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);
                escribirEnLog(sql);

                while (rs.next()) {

                    JSONObject jObject = new JSONObject();
                    jObject.put("id", rs.getInt("id"));
                    jObject.put("estado", rs.getString("ESTADO"));
                    jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
                    jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
                    jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
                    jObject.put("id_cliente", rs.getInt("id_cliente"));
                    jArray.add(jObject);

                }
                st.close();
            } else if (criterio.equalsIgnoreCase("Estado")) {

                String estado = jP.get("Estado").toString();

                String sql = "select * from PEDIDO_PRODUCTO where ESTADO = '" + estado + "'";
                Statement st2 = con.createStatement();
                ResultSet rs = st2.executeQuery(sql);
                escribirEnLog(sql);

                while (rs.next()) {

                    JSONObject jObject = new JSONObject();
                    jObject.put("id", rs.getInt("id"));
                    jObject.put("estado", rs.getString("ESTADO"));
                    jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
                    jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
                    jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
                    jObject.put("id_cliente", rs.getInt("id_cliente"));
                    jArray.add(jObject);
                }

                st2.close();

            } else if (criterio.equalsIgnoreCase("Fecha Solicitud")) {

                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                java.util.Date date = format.parse(jP.get("fecha_solicitud").toString().substring(0, 10));
                System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
                Calendar cEsp = new GregorianCalendar();
                cEsp.setTime(date);

                String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                        + "-" + cEsp.get(GregorianCalendar.YEAR);

                System.out.println("Fecha " + fechaS);
                String sql = "select * from PEDIDO_PRODUCTO where fecha_solicitud = "
                        + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);
                escribirEnLog(sql);

                while (rs.next()) {

                    JSONObject jObject = new JSONObject();
                    jObject.put("id", rs.getInt("id"));
                    jObject.put("estado", rs.getString("ESTADO"));
                    jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
                    jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
                    jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
                    jObject.put("id_cliente", rs.getInt("id_cliente"));
                    jArray.add(jObject);

                }

                st.close();
            } else if (criterio.equalsIgnoreCase("Fecha Entrega")) {

                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                java.util.Date date = format.parse(jP.get("fecha_entrega").toString().substring(0, 10));
                System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
                Calendar cEsp = new GregorianCalendar();
                cEsp.setTime(date);

                String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                        + "-" + cEsp.get(GregorianCalendar.YEAR);

                System.out.println("Fecha " + fechaS);
                String sql = "select * from PEDIDO_PRODUCTO where fecha_entrega = "
                        + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);
                escribirEnLog(sql);

                while (rs.next()) {

                    JSONObject jObject = new JSONObject();
                    jObject.put("id", rs.getInt("id"));
                    jObject.put("estado", rs.getString("ESTADO"));
                    jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
                    jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
                    jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
                    jObject.put("id_cliente", rs.getInt("id_cliente"));
                    jArray.add(jObject);

                }

                st.close();
            } else if (criterio.equalsIgnoreCase("Fecha Esperada Entrega")) {

                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                java.util.Date date = format.parse(jP.get("fecha_esperada_entrega").toString().substring(0, 10));
                System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
                Calendar cEsp = new GregorianCalendar();
                cEsp.setTime(date);

                String fechaS = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                        + "-" + cEsp.get(GregorianCalendar.YEAR);

                System.out.println("Fecha " + fechaS);
                String sql = "select * from PEDIDO_PRODUCTO where fecha_esperada_entrega = "
                        + "TO_DATE('" + fechaS + "','dd-mm-yyyy')";
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);
                escribirEnLog(sql);

                while (rs.next()) {

                    JSONObject jObject = new JSONObject();
                    jObject.put("id", rs.getInt("id"));
                    jObject.put("estado", rs.getString("ESTADO"));
                    jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
                    jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
                    jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
                    jObject.put("id_cliente", rs.getInt("id_cliente"));
                    jArray.add(jObject);

                }

                st.close();
            } else if (criterio.equalsIgnoreCase("Id Cliente")) {

                int id_cliente = (int) jP.get("id_cliente");

                String sql = "select * from PEDIDO_PRODUCTO where ID_CLIENTE = " + id_cliente;
                Statement st2 = con.createStatement();
                ResultSet rs = st2.executeQuery(sql);
                escribirEnLog(sql);

                while (rs.next()) {

                    JSONObject jObject = new JSONObject();
                    jObject.put("id", rs.getInt("id"));
                    jObject.put("estado", rs.getString("ESTADO"));
                    jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
                    jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
                    jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
                    jObject.put("id_cliente", rs.getInt("id_cliente"));
                    jArray.add(jObject);
                }

                st2.close();

            }
            cerrarConexion();
            return jArray;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            throw new Exception("");
        }
    }

    @POST
    @Path("/consultarProveedores")
    public JSONArray consultarProveedores(JSONObject jP) throws Exception {

        JSONArray jArray = new JSONArray();
        abrirConexion();
        String criterio = jP.get("Criterio").toString();
        System.out.println("Criterio: " + criterio);
        if (criterio.equalsIgnoreCase("Documento Id")) {

            String documentoId = jP.get("documentoId").toString();

            String sql = "select * from PROVEEDOR where DOCUMENTO_ID = '" + documentoId + "'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            escribirEnLog(sql);

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                jObject.put("documento_id", rs.getString("documento_id"));
                jObject.put("nombre", rs.getString("nombre"));
                jObject.put("ciudad", rs.getString("ciudad"));
                jObject.put("direccion", rs.getString("direccion"));
                jObject.put("telefono", rs.getString("telefono"));
                jObject.put("volumen_maximo", rs.getInt("volumen_maximo"));
                jObject.put("tiempo_de_entrega", rs.getInt("tiempo_de_entrega"));
                jObject.put("representante_legal", rs.getInt("representante_legal"));
                jArray.add(jObject);

            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Nombre")) {

            String nombre = jP.get("nombre").toString();

            String sql = "select * from PROVEEDOR where NOMBRE LIKE '%" + nombre + "%'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            escribirEnLog(sql);

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                jObject.put("documento_id", rs.getString("documento_id"));
                jObject.put("nombre", rs.getString("nombre"));
                jObject.put("ciudad", rs.getString("ciudad"));
                jObject.put("direccion", rs.getString("direccion"));
                jObject.put("telefono", rs.getString("telefono"));
                jObject.put("volumen_maximo", rs.getInt("volumen_maximo"));
                jObject.put("tiempo_de_entrega", rs.getInt("tiempo_de_entrega"));
                jObject.put("representante_legal", rs.getInt("representante_legal"));
                jArray.add(jObject);

            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Ciudad")) {

            String ciudad = jP.get("ciudad").toString();

            String sql = "select * from PROVEEDOR where CIUDAD LIKE '%" + ciudad + "%'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            escribirEnLog(sql);

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                jObject.put("documento_id", rs.getString("documento_id"));
                jObject.put("nombre", rs.getString("nombre"));
                jObject.put("ciudad", rs.getString("ciudad"));
                jObject.put("direccion", rs.getString("direccion"));
                jObject.put("telefono", rs.getString("telefono"));
                jObject.put("volumen_maximo", rs.getInt("volumen_maximo"));
                jObject.put("tiempo_de_entrega", rs.getInt("tiempo_de_entrega"));
                jObject.put("representante_legal", rs.getInt("representante_legal"));
                jArray.add(jObject);

            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Direccion")) {

            String direccion = jP.get("direccion").toString();

            String sql = "select * from PROVEEDOR where DIRECCION LIKE '%" + direccion + "%'";
            System.out.println("---------------------------------Query------------------------"
                    + "-----------------------------------------------------");
            System.out.println(sql);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            escribirEnLog(sql);

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                jObject.put("documento_id", rs.getString("documento_id"));
                jObject.put("nombre", rs.getString("nombre"));
                jObject.put("ciudad", rs.getString("ciudad"));
                jObject.put("direccion", rs.getString("direccion"));
                jObject.put("telefono", rs.getString("telefono"));
                jObject.put("volumen_maximo", rs.getInt("volumen_maximo"));
                jObject.put("tiempo_de_entrega", rs.getInt("tiempo_de_entrega"));
                jObject.put("representante_legal", rs.getInt("representante_legal"));
                jArray.add(jObject);

            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Telefono")) {

            String telefono = jP.get("telefono").toString();

            String sql = "select * from PROVEEDOR where TELEFONO LIKE '%" + telefono + "%'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            escribirEnLog(sql);

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                jObject.put("documento_id", rs.getString("documento_id"));
                jObject.put("nombre", rs.getString("nombre"));
                jObject.put("ciudad", rs.getString("ciudad"));
                jObject.put("direccion", rs.getString("direccion"));
                jObject.put("telefono", rs.getString("telefono"));
                jObject.put("volumen_maximo", rs.getInt("volumen_maximo"));
                jObject.put("tiempo_de_entrega", rs.getInt("tiempo_de_entrega"));
                jObject.put("representante_legal", rs.getInt("representante_legal"));
                jArray.add(jObject);

            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Volumen Maximo")) {

            int volMax = (int) jP.get("volumen_maximo");

            String sql = "select * from PROVEEDOR where volumen_maximo = " + volMax;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            escribirEnLog(sql);

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                jObject.put("documento_id", rs.getString("documento_id"));
                jObject.put("nombre", rs.getString("nombre"));
                jObject.put("ciudad", rs.getString("ciudad"));
                jObject.put("direccion", rs.getString("direccion"));
                jObject.put("telefono", rs.getString("telefono"));
                jObject.put("volumen_maximo", rs.getInt("volumen_maximo"));
                jObject.put("tiempo_de_entrega", rs.getInt("tiempo_de_entrega"));
                jObject.put("representante_legal", rs.getInt("representante_legal"));
                jArray.add(jObject);

            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Tiempo Entrega")) {

            int tiempoEntrega = (int) jP.get("tiempo_de_entrega");

            String sql = "select * from PROVEEDOR where tiempo_de_entrega = " + tiempoEntrega;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            escribirEnLog(sql);

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                jObject.put("documento_id", rs.getString("documento_id"));
                jObject.put("nombre", rs.getString("nombre"));
                jObject.put("ciudad", rs.getString("ciudad"));
                jObject.put("direccion", rs.getString("direccion"));
                jObject.put("telefono", rs.getString("telefono"));
                jObject.put("volumen_maximo", rs.getInt("volumen_maximo"));
                jObject.put("tiempo_de_entrega", rs.getInt("tiempo_de_entrega"));
                jObject.put("representante_legal", rs.getInt("representante_legal"));
                jArray.add(jObject);

            }
            st.close();
        } else if (criterio.equalsIgnoreCase("Representante Legal")) {

            int representanteLegal = (int) jP.get("representante_legal");

            String sql = "select * from PROVEEDOR where representante_legal = " + representanteLegal;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            escribirEnLog(sql);

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                jObject.put("documento_id", rs.getString("documento_id"));
                jObject.put("nombre", rs.getString("nombre"));
                jObject.put("ciudad", rs.getString("ciudad"));
                jObject.put("direccion", rs.getString("direccion"));
                jObject.put("telefono", rs.getString("telefono"));
                jObject.put("volumen_maximo", rs.getInt("volumen_maximo"));
                jObject.put("tiempo_de_entrega", rs.getInt("tiempo_de_entrega"));
                jObject.put("representante_legal", rs.getInt("representante_legal"));
                jArray.add(jObject);

            }
            st.close();
        }

        cerrarConexion();
        return jArray;

    }

    @POST
    @Path("/desactivarEstacion")
    public JSONObject desactivarEstacion(JSONObject jO) throws Exception {

        abrirConexion();

        // Contar activos st2
        String query2 = "select count(*) as cuenta from estacion where ESTADO='activo'";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query2);
        Statement st2 = con.createStatement();
        ResultSet rs2 = st2.executeQuery(query2);
        int num_est_activas = 0;
        while (rs2.next()) {
            num_est_activas = rs2.getInt("CUENTA");
            System.out.println("num_est_activas: " + num_est_activas);
        }
        st2.close();

        // Contar etapas st3
        String query3 = "select count(*) as cuenta from etapa_de_produccion";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query3);
        Statement st3 = con.createStatement();
        ResultSet rs3 = st3.executeQuery(query3);
        int num_etapas = 0;
        while (rs3.next()) {
            num_etapas = rs3.getInt("CUENTA");
            System.out.println("num_etapas: " + num_etapas);
        }
        st3.close();

        //  Verificacion: no hay estaciones activas
        if (num_est_activas == 1) {
            JSONObject jRespuesta = new JSONObject();
            jRespuesta.put("Respuesta", "No se puede borrar estacion");
            rollback();
            return jRespuesta;
        }

        // Cambiar estado st1
        int estacion_codigo = Integer.parseInt(jO.get("codigo").toString());
        String query1 = "UPDATE ESTACION SET ESTADO = 'inactivo' WHERE CODIGO = " + estacion_codigo;
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query1);
        System.out.println("Estaciones actualizadas");
        Statement st1 = con.createStatement();
        st1.executeUpdate(query1);
        st1.close();

        // Borrar relaciones etapa_estacion st4
        String query4 = "DELETE FROM ETAPA_ESTACION";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query4);
        Statement st4 = con.createStatement();
        st4.executeUpdate(query4);
        st4.close();

        // Seleccionar etapas st5
        String query5 = ("select * from etapa_de_produccion");
        System.out.println(query5);
        Statement st5 = con.createStatement();
        ResultSet rs5 = st5.executeQuery(query5);
        int etapas[] = new int[num_etapas];
        int temp = 0;
        int i = 0;
        while (rs5.next()) {
            System.out.println("indice: " + i);
            temp = rs5.getInt("NUMERO_SECUENCIA");
            System.out.println("etapas[" + i + "]" + " = " + temp);
            etapas[i] = temp;
            i++;
        }
        System.out.println(etapas);
        st5.close();

        // Seleccionar estaciones st6
        String query6 = ("select * from estacion where ESTADO = 'activo'");
        System.out.println(query6);
        Statement st6 = con.createStatement();
        ResultSet rs6 = st6.executeQuery(query6);
        int estaciones[] = new int[num_est_activas - 1];
        temp = 0;
        i = 0;
        while (rs6.next()) {
            temp = rs6.getInt("CODIGO");
            System.out.println("estaciones[" + i + "]" + " = " + temp);
            estaciones[i] = temp;
            i++;
        }
        System.out.println(estaciones);
        st6.close();

        // Crear relaciones st7
        String query7 = ("");
        System.out.println(query7);
        Statement st7 = null;
        int i_estacion = 0;
        for (int k = 0; k < etapas.length; k++) {
            if (i_estacion == estaciones.length) {
                i_estacion = 0;
            }
            System.out.println("Etapa: " + k);
            System.out.println("Estacion: " + i_estacion);
            System.out.println("etapas[k] " + etapas[k]);
            System.out.println("estaciones[i_estacion] " + estaciones[i_estacion]);
            query7 = ("INSERT INTO ETAPA_ESTACION (ETAPA_ID, ESTACION_ID) VALUES ('" + etapas[k] + "', '" + estaciones[i_estacion] + "')");
            System.out.println(query7);
            st7 = con.createStatement();
            st7.executeUpdate(query7);
            i_estacion++;
        }

        // Cerrar conexion
        cerrarConexion();
        JSONObject jRespuestaOk = new JSONObject();
        jRespuestaOk.put("Respuesta", "Proceso correcto");
        return jRespuestaOk;
    }

    // activarEstacion
    @POST
    @Path("/activarEstacion")
    public JSONObject activarEstacion(JSONObject jO) throws Exception {

        abrirConexion();

        // Contar activos st2
        String query2 = "select count(*) as cuenta from estacion where ESTADO='activo'";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query2);
        Statement st2 = con.createStatement();
        ResultSet rs2 = st2.executeQuery(query2);
        int num_est_activas = 0;
        while (rs2.next()) {
            num_est_activas = rs2.getInt("CUENTA");
            System.out.println("num_est_activas: " + num_est_activas);
        }
        st2.close();

        // Contar etapas st3
        String query3 = "select count(*) as cuenta from etapa_de_produccion";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query3);
        Statement st3 = con.createStatement();
        ResultSet rs3 = st3.executeQuery(query3);
        int num_etapas = 0;
        while (rs3.next()) {
            num_etapas = rs3.getInt("CUENTA");
            System.out.println("num_etapas: " + num_etapas);
        }
        st3.close();

        //  Verificacion: no hay estaciones activas
        /*
         if(num_est_activas == 1)
         {
         JSONObject jRespuesta =  new JSONObject();
         jRespuesta.put("Respuesta", "No se puede borrar estacion");
         rollback();
         return jRespuesta;
         }
         */
        // Cambiar estado st1
        int estacion_codigo = Integer.parseInt(jO.get("codigo").toString());
        String query1 = "UPDATE ESTACION SET ESTADO = 'activo' WHERE CODIGO = " + estacion_codigo;
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query1);
        System.out.println("Estaciones actualizadas");
        Statement st1 = con.createStatement();
        st1.executeUpdate(query1);
        st1.close();

        // Borrar relaciones etapa_estacion st4
        String query4 = "DELETE FROM ETAPA_ESTACION";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(query4);
        Statement st4 = con.createStatement();
        st4.executeUpdate(query4);
        st4.close();

        // Seleccionar etapas st5
        String query5 = ("select * from etapa_de_produccion");
        System.out.println(query5);
        Statement st5 = con.createStatement();
        ResultSet rs5 = st5.executeQuery(query5);
        int etapas[] = new int[num_etapas];
        int temp = 0;
        int i = 0;
        while (rs5.next()) {
            System.out.println("indice: " + i);
            temp = rs5.getInt("NUMERO_SECUENCIA");
            System.out.println("etapas[" + i + "]" + " = " + temp);
            etapas[i] = temp;
            i++;
        }
        System.out.println(etapas);
        st5.close();

        // Seleccionar estaciones st6
        String query6 = ("select * from estacion where ESTADO = 'activo'");
        System.out.println(query6);
        Statement st6 = con.createStatement();
        ResultSet rs6 = st6.executeQuery(query6);
        int estaciones[] = new int[num_est_activas + 1];
        temp = 0;
        i = 0;
        while (rs6.next()) {
            temp = rs6.getInt("CODIGO");
            System.out.println("estaciones[" + i + "]" + " = " + temp);
            estaciones[i] = temp;
            i++;
        }
        System.out.println(estaciones);
        st6.close();

        // Crear relaciones st7
        String query7 = ("");
        System.out.println(query7);
        Statement st7 = null;
        int i_estacion = 0;
        for (int k = 0; k < etapas.length; k++) {
            if (i_estacion == estaciones.length) {
                i_estacion = 0;
            }
            System.out.println("Etapa: " + k);
            System.out.println("Estacion: " + i_estacion);
            System.out.println("etapas[k] " + etapas[k]);
            System.out.println("estaciones[i_estacion] " + estaciones[i_estacion]);
            query7 = ("INSERT INTO ETAPA_ESTACION (ETAPA_ID, ESTACION_ID) VALUES ('" + etapas[k] + "', '" + estaciones[i_estacion] + "')");
            System.out.println(query7);
            st7 = con.createStatement();
            st7.executeUpdate(query7);
            i_estacion++;
        }

        // Cerrar conexion
        cerrarConexion();
        JSONObject jRespuestaOk = new JSONObject();
        jRespuestaOk.put("Respuesta", "Proceso correcto");
        return jRespuestaOk;
    }

    @POST
    @Path("/verPedido")
    public JSONObject verPedido(JSONObject jP) throws Exception {
        try {
            int idPedido = (int) jP.get("id_pedido");
            JSONObject jO = new JSONObject();
            abrirConexion();
            String sql = "select * from PEDIDO_PRODUCTO where ID = " + idPedido;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            escribirEnLog(sql);
            if (rs.next()) {

                jO.put("id", rs.getInt("id"));
                jO.put("estado", rs.getString("estado"));
                jO.put("cantidad_producto", rs.getInt("cantidad_producto"));
                jO.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
                jO.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
                jO.put("fecha_entrega", rs.getDate("fecha_entrega"));

                sql = "select CLIENTE.NOMBRE,CLIENTE.NUMERO_REGISTRO,CLIENTE.CIUDAD,CLIENTE.DIRECCION"
                        + ",CLIENTE.REPRESENTANTE_LEGAL,CLIENTE.TELEFONO from PEDIDO_PRODUCTO inner join "
                        + "CLIENTE on PEDIDO_PRODUCTO.ID_CLIENTE=CLIENTE.NUMERO_REGISTRO WHERE "
                        + "PEDIDO_PRODUCTO.ID=" + idPedido;
                System.out.println("-----------------------------------------------------------------");
                System.out.println("Ver Pedido - Cliente");
                System.out.println(sql);
                System.out.println("-----------------------------------------------------------------");
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);
                escribirEnLog(sql);
                // Cliente
                JSONObject jCliente = new JSONObject();
                if (rs2.next()) {

                    jCliente.put("numero_registro", rs2.getInt("numero_registro"));
                    jCliente.put("nombre", rs2.getString("nombre"));
                    jCliente.put("ciudad", rs2.getString("ciudad"));
                    jCliente.put("direccion", rs2.getString("direccion"));
                    jCliente.put("telefono", rs2.getString("telefono"));
                    jCliente.put("representante_legal", rs2.getInt("representante_legal"));

                }
                jO.put("cliente", jCliente);
                st2.close();
                // Items
                sql = "select * from ITEM inner join PRODUCTO on ITEM.NOMBRE_PRODUCTO=PRODUCTO.NOMBRE "
                        + "where ID_PEDIDO=" + idPedido;
                st2 = con.createStatement();
                rs2 = st2.executeQuery(sql);

                escribirEnLog(sql);
                JSONArray jItems = new JSONArray();
                while (rs2.next()) {

                    JSONObject jItem = new JSONObject();
                    jItem.put("id", rs2.getInt("id"));
                    jItem.put("nombre_producto", rs2.getString("nombre_producto"));
                    jItem.put("estado", rs2.getString("estado"));
                    jItem.put("etapa", rs2.getInt("etapa"));
                    jItem.put("costo_de_venta", rs2.getInt("costo_de_venta"));
                    jItems.add(jItem);
                }
                jO.put("productos", jItems);
                st2.close();
                //Materias Primas
                sql = "select (items*cantidad_unidades)as cantidad,id_materia_prima from "
                        + "(select ITEM.NOMBRE_PRODUCTO as id_producto, count(*) as items "
                        + "from ITEM where ID_PEDIDO=" + idPedido + " group by NOMBRE_PRODUCTO)"
                        + " natural inner join MATERIAS_PRIMAS_PRODUCTO";
                st2 = con.createStatement();
                rs2 = st2.executeQuery(sql);

                escribirEnLog(sql);
                JSONArray jMateriasPrimas = new JSONArray();
                while (rs2.next()) {

                    JSONObject jMateriaPrima = new JSONObject();
                    jMateriaPrima.put("materia", rs2.getString("id_materia_prima"));
                    jMateriaPrima.put("cantidad", rs2.getInt("cantidad"));
                    jMateriasPrimas.add(jMateriaPrima);
                }
                jO.put("materias_primas", jMateriasPrimas);
                st2.close();

                //Componentes
                sql = "select * from COMPONENTE_ITEM where ID_PEDIDO_PRODUCTO=" + idPedido;
                st2 = con.createStatement();
                rs2 = st2.executeQuery(sql);

                escribirEnLog(sql);

                JSONArray jComponentes = new JSONArray();
                while (rs2.next()) {

                    JSONObject jComponente = new JSONObject();
                    jComponente.put("id", rs2.getInt("id"));
                    jComponente.put("componente", rs2.getString("componente"));
                    jComponente.put("unidades", rs2.getInt("unidades"));
                    jComponente.put("estado", rs2.getString("estado"));
                    jComponentes.add(jComponente);
                }
                jO.put("componentes", jComponentes);
                st2.close();

            }

            st.close();
            return jO;
        } catch (Exception e) {
            rollback();
            e.printStackTrace();
            throw new Exception("Error en la base de datos");
        }
    }

    @POST
    @Path("/verProveedor")
    public JSONObject verProveedor(JSONObject jP) throws Exception {
        try {
            String idProveedor = jP.get("id_proveedor").toString();
            abrirConexion();
            JSONObject jResp = new JSONObject();

            String sql = "select * from PROVEEDOR where DOCUMENTO_ID='" + idProveedor + "'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            escribirEnLog(sql);
            if (rs.next()) {
                jResp.put("documento_id", rs.getString("documento_id"));
                jResp.put("nombre", rs.getString("nombre"));
                jResp.put("ciudad", rs.getString("ciudad"));
                jResp.put("direccion", rs.getString("direccion"));
                jResp.put("telefono", rs.getString("telefono"));
                jResp.put("volumen_maximo", rs.getInt("volumen_maximo"));
                jResp.put("tiempo_de_entrega", rs.getInt("tiempo_de_entrega"));
                jResp.put("representante_legal", rs.getInt("representante_legal"));

                //Materias Primas
                sql = "select * from MATERIA_PRIMA where PROOVEDOR_ID ='" + idProveedor + "'";
                Statement st2 = con.createStatement();
                ResultSet rs2 = st2.executeQuery(sql);

                escribirEnLog(sql);
                JSONArray jMaterias = new JSONArray();
                while (rs2.next()) {
                    JSONObject jMateria = new JSONObject();
                    jMateria.put("nombre", rs2.getString("nombre"));
                    jMateria.put("u_medida", rs2.getString("u_medida"));
                    jMateria.put("tipo", rs2.getString("tipo"));
                    jMaterias.add(jMateria);
                }
                jResp.put("materias_primas", jMaterias);
                st2.close();

                //Componentes
                sql = "select * from COMPONENTE where PROOVEDOR_ID ='" + idProveedor + "'";
                st2 = con.createStatement();
                rs2 = st2.executeQuery(sql);

                escribirEnLog(sql);
                JSONArray jComponentes = new JSONArray();
                while (rs2.next()) {
                    JSONObject jComponente = new JSONObject();
                    jComponente.put("nombre", rs2.getString("nombre"));
                    jComponente.put("tipo", rs2.getString("tipo"));
                    jComponentes.add(jComponente);
                }
                jResp.put("componentes", jComponentes);
                st2.close();

                // Productos
                sql = "select * from MATERIAS_PRIMAS_PRODUCTO inner join MATERIA_PRIMA on "
                        + "MATERIAS_PRIMAS_PRODUCTO.ID_MATERIA_PRIMA=MATERIA_PRIMA.NOMBRE where "
                        + "MATERIA_PRIMA.PROOVEDOR_ID='" + idProveedor + "'";
                st2 = con.createStatement();
                rs2 = st2.executeQuery(sql);

                escribirEnLog(sql);
                JSONArray jProductos = new JSONArray();
                while (rs2.next()) {
                    JSONObject jProducto = new JSONObject();
                    jProducto.put("id_producto", rs2.getString("id_producto"));
                    jProducto.put("cantidad_unidades", rs2.getInt("cantidad_unidades"));
                    jProductos.add(jProducto);
                }
                jResp.put("productos", jProductos);
                st2.close();
            }
            st.close();
            return jResp;
        } catch (Exception e) {
            rollback();
            e.printStackTrace();
            throw new Exception("Error en la base de datos");
        }

    }

    @POST
    @Path("/consultarClientes")
    public JSONArray consultarCliente(JSONObject jP) throws Exception {

        JSONArray jArray = new JSONArray();
        abrirConexion();
        System.out.println(jP);
        String criterio = jP.get("Criterio").toString();
        System.out.println("Criterio: " + criterio);
        if (criterio.equalsIgnoreCase("Nombre")) {

            String nombre = jP.get("Nombre").toString();

            String sql = "select * from CLIENTE inner join PEDIDO_PRODUCTO on CLIENTE.NUMERO_REGISTRO = PEDIDO_PRODUCTO.ID_CLIENTE where NOMBRE LIKE '%" + nombre + "%'";
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(sql);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int num_act = -1;
            int num = -1;

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                num = rs.getInt("numero_registro");
                if (num != num_act) {
                    jObject.put("numero_registro", num);
                    jObject.put("nombre", rs.getString("nombre"));
                    jObject.put("ciudad", rs.getString("ciudad"));
                    jObject.put("direccion", rs.getString("direccion"));
                    jObject.put("telefono", rs.getString("telefono"));
                    jObject.put("representante_legal", rs.getInt("representante_legal"));
                    num_act = num;
                } else {
                    jObject.put("numero_registro", "");
                    jObject.put("nombre", "");
                    jObject.put("ciudad", "");
                    jObject.put("direccion", "");
                    jObject.put("telefono", "");
                    jObject.put("representante_legal", "");
                }
                jObject.put("id", rs.getInt("id"));
                jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
                jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
                jObject.put("estado", rs.getString("estado"));
                jObject.put("cantidad_producto", rs.getInt("cantidad_producto"));
                jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
                jArray.add(jObject);
            }
            st.close();
        } else {

            String ciudad = jP.get("Ciudad").toString();

            String sql = "select * from CLIENTE inner join PEDIDO_PRODUCTO on CLIENTE.NUMERO_REGISTRO = PEDIDO_PRODUCTO.ID_CLIENTE where CIUDAD LIKE '%" + ciudad + "%'";
            System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
            System.out.println(sql);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int num_act = -1;
            int num = -1;

            while (rs.next()) {

                JSONObject jObject = new JSONObject();
                num = rs.getInt("numero_registro");
                if (num != num_act) {
                    jObject.put("numero_registro", num);
                    jObject.put("nombre", rs.getString("nombre"));
                    jObject.put("ciudad", rs.getString("ciudad"));
                    jObject.put("direccion", rs.getString("direccion"));
                    jObject.put("telefono", rs.getString("telefono"));
                    jObject.put("representante_legal", rs.getInt("representante_legal"));
                    num_act = num;
                } else {
                    jObject.put("numero_registro", "");
                    jObject.put("nombre", "");
                    jObject.put("ciudad", "");
                    jObject.put("direccion", "");
                    jObject.put("telefono", "");
                    jObject.put("representante_legal", "");
                }
                jObject.put("id", rs.getInt("id"));
                jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
                jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
                jObject.put("estado", rs.getString("estado"));
                jObject.put("cantidad_producto", rs.getInt("cantidad_producto"));
                jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
                jArray.add(jObject);

            }
            st.close();
        }
        cerrarConexion();
        return jArray;

    }

    @POST
    @Path("/consultarPedidosRFC10")
    public JSONArray consultarPedidosRFC10(JSONObject jP) throws Exception {

        JSONArray jArray = new JSONArray();
        abrirConexion();
        System.out.println("Parametro de consultarPedidosRFC10");
        System.out.println(jP);
        String tipo = jP.get("Tipo").toString();
        System.out.println("Tipo: " + tipo);
        int costo = Integer.parseInt(jP.get("Costo").toString());
        System.out.println("Costo: " + costo);

        String sql = "select * from PEDIDO_PRODUCTO "
                + "where ID IN ("
                + "select ID from ("
                + "select * from PEDIDO_PRODUCTO "
                + "inner join PEDIDO_USA_MATERIA_PRIMA "
                + "on ID = ID_PEDIDO) "
                + "inner join MATERIA_PRIMA "
                + "on NOMBRE = NOMBRE_MATERIA_PRIMA "
                + "where PRECIO > " + costo + " AND tipo = '" + tipo + "')";

        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(sql);
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            JSONObject jObject = new JSONObject();
            jObject.put("id", rs.getInt("id"));
            jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
            jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
            jObject.put("estado", rs.getString("estado"));
            jObject.put("cantidad_producto", rs.getInt("cantidad_producto"));
            jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
            jArray.add(jObject);
        }
        st.close();
        cerrarConexion();
        return jArray;

    }

    @POST
    @Path("/consultarPedidosRFC11")
    public JSONArray consultarPedidosRFC11(JSONObject jP) throws Exception {

        JSONArray jArray = new JSONArray();
        abrirConexion();
        System.out.println("Parametro de consultarPedidosRFC11");
        System.out.println(jP);
        String material = jP.get("Material").toString();
        System.out.println("Material: " + material);

        String sql = "select * from PEDIDO_PRODUCTO "
                + "where ID IN ("
                + "select ID from ("
                + "select * from PEDIDO_PRODUCTO "
                + "inner join PEDIDO_USA_MATERIA_PRIMA "
                + "on ID = ID_PEDIDO) "
                + "inner join MATERIA_PRIMA "
                + "on NOMBRE = NOMBRE_MATERIA_PRIMA "
                + "where NOMBRE = '" + material + "')";

        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(sql);
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            JSONObject jObject = new JSONObject();
            jObject.put("id", rs.getInt("id"));
            jObject.put("fecha_esperada_entrega", rs.getDate("fecha_esperada_entrega"));
            jObject.put("fecha_entrega", rs.getDate("fecha_entrega"));
            jObject.put("estado", rs.getString("estado"));
            jObject.put("cantidad_producto", rs.getInt("cantidad_producto"));
            jObject.put("fecha_solicitud", rs.getDate("fecha_solicitud"));
            jArray.add(jObject);
        }
        st.close();
        cerrarConexion();
        return jArray;

    }

    public void escribirEnLog(String instruccion) {
//        try {
//
//            File file = new File("H:\\logs\\log.txt");
//
//            // if file doesnt exists, then create it
//            //if (!file.exists()) {
//            //	file.createNewFile();
//            //}
//            FileOutputStream fos = new FileOutputStream(file, true);
//            PrintWriter out = new PrintWriter(fos);
//            //FileWriter fw = new FileWriter(file.getAbsoluteFile());
//            //BufferedWriter bw = new BufferedWriter(fw);
//            out.println(instruccion);
//            out.close();
//
//            System.out.println("Done");
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    @POST
    @Path("/consultarEtapasRangoFechaRFC8y9")
    public JSONObject consultarEtapasRangoFechaRFC8y9(JSONObject jP) throws Exception {
        try {
            abrirConexion();
            JSONObject jreturn = new JSONObject();
            String igualdad = (jP.get("igualdad").toString().equals("SI")) ? "=" : "!=";
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha1").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fecha1 = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1) + "-"
                    + cEsp.get(GregorianCalendar.YEAR);
            String fecha1Env = (cEsp.get(GregorianCalendar.MONTH) + 1) + "/" + (cEsp.get(GregorianCalendar.DAY_OF_MONTH)) + "/"
                    + cEsp.get(GregorianCalendar.YEAR);

            date = format.parse(jP.get("fecha2").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fecha2 = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1) + "-"
                    + cEsp.get(GregorianCalendar.YEAR);
            String fecha2Env = (cEsp.get(GregorianCalendar.MONTH) + 1) + "/" + (cEsp.get(GregorianCalendar.DAY_OF_MONTH)) + "/"
                    + cEsp.get(GregorianCalendar.YEAR);

            String criterio = jP.get("criterio").toString();
            String valor = jP.get("valor").toString();

            JSONArray jResp = new JSONArray();

            // RC12-solicitud-id-fechaInicial-FechaFinal
            Send send = new Send();
            String mensaje = "RC12-" + criterio + "-" + valor + "-" + fecha1Env + "-" + fecha2Env;
            System.out.println("Mensaje a enviar " + mensaje);
            send.enviar(mensaje);
            send.close();
            if (criterio.equals("materia prima")) {

                Statement st = con.createStatement();
                String sql = "select* from "
                        + "(select * from materias_primas_producto where id_materia_prima" + igualdad + "'" + valor + "') "
                        + " inner join "
                        + " (select * from "
                        + "  (select codigo_secuencia from ETAPA_FECHA where FECHA>=TO_DATE('" + fecha1 + "','dd-mm-yyyy') "
                        + "AND FECHA<=TO_DATE('" + fecha2 + "','dd-mm-yyyy') "
                        + "  group by CODIGO_SECUENCIA)c  "
                        + "   inner join "
                        + "   ETAPA_DE_PRODUCCION "
                        + "   on ETAPA_DE_PRODUCCION.numero_secuencia=c.codigo_secuencia) "
                        + "  on "
                        + "  id_producto = nombre_producto";
                System.out.println("RFC8 ----------- QUERY\n" + sql);

                ResultSet rs = st.executeQuery(sql);

                while (rs.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id_producto", rs.getString("id_producto"));
                    jO.put("id_etapa", rs.getInt("numero_secuencia"));
                    jO.put("descripcion", rs.getString("descripcion"));
                    jO.put("numero_etapa", rs.getInt("etapa"));
                    jO.put("materia_prima", rs.getString("id_materia_prima"));
                    jResp.add(jO);
                }
            } else if (criterio.equals("tipo material")) {
                Statement st = con.createStatement();
                String sql = "select* from      \n"
                        + "      (select id_producto,tipo  from MATERIA_PRIMA inner join MATERIAS_PRIMAS_PRODUCTO on "
                        + "NOMBRE=ID_MATERIA_PRIMA \n"
                        + "                  where tipo" + igualdad + "'" + valor + "' \n"
                        + "                  )\n"
                        + "      inner join \n"
                        + "      (select * from \n"
                        + "            (select codigo_secuencia from ETAPA_FECHA where FECHA>=TO_DATE('" + fecha1 + "','dd-mm-yyyy') "
                        + "AND FECHA<=TO_DATE('" + fecha2 + "','dd-mm-yyyy') \n"
                        + "                      group by CODIGO_SECUENCIA)c \n"
                        + "            inner join\n"
                        + "            ETAPA_DE_PRODUCCION\n"
                        + "            on ETAPA_DE_PRODUCCION.numero_secuencia=c.codigo_secuencia)\n"
                        + "      on\n"
                        + "      id_producto = nombre_producto";
                System.out.println("RFC8 ----------- QUERY\n" + sql);

                ResultSet rs = st.executeQuery(sql);

                while (rs.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id_producto", rs.getString("id_producto"));
                    jO.put("id_etapa", rs.getInt("numero_secuencia"));
                    jO.put("descripcion", rs.getString("descripcion"));
                    jO.put("numero_etapa", rs.getInt("etapa"));
                    jO.put("tipo_material", rs.getString("tipo"));
                    jResp.add(jO);
                }

            } else if (criterio.equals("pedido")) {
                Statement st = con.createStatement();
                String sql = "select* from      \n"
                        + "      (select id_producto,id_pedido from ITEM inner join MATERIAS_PRIMAS_PRODUCTO on "
                        + "ITEM.NOMBRE_PRODUCTO=MATERIAS_PRIMAS_PRODUCTO.ID_PRODUCTO\n"
                        + "        where ITEM.ID_PEDIDO" + igualdad + valor + "\n"
                        + "                  )\n"
                        + "      inner join \n"
                        + "      (select * from \n"
                        + "            (select codigo_secuencia from ETAPA_FECHA where FECHA>=TO_DATE('" + fecha1 + "','dd-mm-yyyy') "
                        + "AND FECHA<=TO_DATE('" + fecha2 + "','dd-mm-yyyy') \n"
                        + "                      group by CODIGO_SECUENCIA)c \n"
                        + "            inner join\n"
                        + "            ETAPA_DE_PRODUCCION\n"
                        + "            on ETAPA_DE_PRODUCCION.numero_secuencia=c.codigo_secuencia)\n"
                        + "      on\n"
                        + "      id_producto = nombre_producto";
                System.out.println("RFC8 ----------- QUERY\n" + sql);

                ResultSet rs = st.executeQuery(sql);

                while (rs.next()) {
                    JSONObject jO = new JSONObject();
                    jO.put("id_producto", rs.getString("id_producto"));
                    jO.put("id_etapa", rs.getInt("numero_secuencia"));
                    jO.put("descripcion", rs.getString("descripcion"));
                    jO.put("numero_etapa", rs.getInt("etapa"));
                    jO.put("id_pedido", rs.getInt("id_pedido"));
                    jResp.add(jO);
                }

            }
            Long milis = System.currentTimeMillis();
            while (System.currentTimeMillis() - milis < 10000) {
                for (int i = 0; i < buzon.size(); i++) {
                    if (buzon.get(i).startsWith("RFC12R$")) {
                        // RFC12$elemento1Tupla1-elemento2Tupla1-elemento3Tupla1/elemento1Tupla2-elemento2Tupla2

                        String str = buzon.get(i).substring(7);
                        buzon.remove(i);
                        jreturn.put("otraLista", str);
                    }
                }
            }

            cerrarConexion();
            jreturn.put("lista", jResp);
            return jreturn;
        } catch (Exception e) {
            rollback();
            throw e;
        }

    }

    @GET
    @Path("/inicializarColas")
    public String inicializarColas() throws JMSException, NamingException {
        InitialContext init = new InitialContext();
        this.cf = (ConnectionFactory) init.lookup("RemoteConnectionFactory");
        this.d = (Destination) init.lookup("queue/queue1");
        this.c = (javax.jms.Connection) this.cf.createConnection("guest123", "guest");
        ((javax.jms.Connection) this.c).start();
        this.s = ((javax.jms.Connection) this.c).createSession(false, Session.AUTO_ACKNOWLEDGE);
        mc = s.createConsumer(d);
        this.mc.setMessageListener(this);
        buzon = new ArrayList<String>();
        return "Inicializo bien";
    }

    public String receive() throws JMSException {
        TextMessage msg = (TextMessage) mc.receive();
        return msg.getText();
    }

    public void close() throws JMSException {
        this.c.close();
    }

    public void onMessage(Message message) {
        try {
            TextMessage text = (TextMessage) message;
            String respuesta = text.getText();
            String txt = respuesta;
            System.out.println("El mensaje de Jose fue: " + respuesta);
            System.out.println("onMessage");
            System.out.println(respuesta.startsWith("jp-pe"));
            System.out.println(respuesta.equals("jp-pe"));
            if (txt.startsWith("RF18-")) {

                String[] params = txt.split("-");
                String[] fecha = params[1].split("/");
                Calendar c = new GregorianCalendar(Integer.parseInt(fecha[2]), Integer.parseInt(fecha[0]), Integer.parseInt(fecha[1]));

                JSONObject jp = new JSONObject();

                jp.put("fechaEsperada", fecha[2]+"-"+((fecha[0].length()==1)?("0"+fecha[0]):fecha[0])+"-"+fecha[1]);
                jp.put("nombre", params[2]);
                jp.put("cantidad", params[3]);
                jp.put("id_cliente", params[4]);

                JSONObject jO = registrarPedido2(jp);

                Send env = new Send();
                env.enviar("RF18R-" + jO.get("id_pedido") + "-" + jO.get("Respuesta"));
                env.close();
            } else if (txt.startsWith("RF18R-")) {

                buzon.add(txt);
            } else if (txt.equals("jp-pe")) {
                System.out.println("Entro a jp-pe");
                Send env = new Send();
                env.enviar(darEtapasCuentaJose());
                env.close();
            } else if (txt.equals("jp-r")) {
                System.out.println("Entro a jp-r");
                buzon.add(txt);
            } else if (txt.equals("RFC12R$")) {
                System.out.println("Entro a RFC12R$");
                buzon.add(txt);
            } else if (txt.equals("RC12-")) {
                System.out.println("Entro a RC12-");
                //RC12-solicitud-id-fechaInicial-FechaFinal
                String s[] = txt.split("-");
                String criterio = s[1];
                String valor = s[2];
                String fecha[] = s[3].split("/");
                Calendar c = new GregorianCalendar(Integer.parseInt(fecha[2]), Integer.parseInt(fecha[0]), Integer.parseInt(fecha[1]));
                Date fechaI = c.getTime();
                fecha = s[4].split("/");
                c = new GregorianCalendar(Integer.parseInt(fecha[2]), Integer.parseInt(fecha[0]), Integer.parseInt(fecha[1]));
                Date fechaF = c.getTime();

                JSONObject jParam = new JSONObject();
                jParam.put("criterio", criterio);
                jParam.put("valor", valor);
                jParam.put("fecha1", fechaI);
                jParam.put("fecha2", fechaF);
                String mensaje = consultarEtapasRangoFechaRFC8y9AEnviar(jParam);
                System.out.println("Respuesta a enviar RFC12: " + mensaje);
                //Enviar mis etapas
                Send send = new Send();
                send.enviar("RFC12$" + mensaje);
                send.close();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GET
    @Path("/metodo4")
    public String metodo4() {
        try {
            Send env = new Send();
            env.enviar("Mensaje de Jonathan y Francisco");
            env.close();
            return "Bien";

        } catch (Exception e) {
            e.printStackTrace();
            return "Mal";
        }

    }

    public JSONObject registrarPedido2(JSONObject jO) throws Exception {
        try {
            JSONObject jRespuesta = new JSONObject();
            String resp = "";
            System.out.println("Registrar Pedido 2 "+jO.toJSONString());
            abrirConexion();

            
            String lock = "lock table " + "pedido_producto" + " in exclusive mode";
            Statement stmt1 = con.createStatement();
            stmt1.execute(lock);
            stmt1.close();

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            System.out.println(jO.get("fechaEsperada").toString());
            java.util.Date date = format.parse(jO.get("fechaEsperada").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String sFecha = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1)
                    + "-" + cEsp.get(GregorianCalendar.YEAR);

            System.out.println("Fecha: " + sFecha);

            String nombreProducto = jO.get("nombre").toString();
            int cantidad = Integer.parseInt(jO.get("cantidad").toString());
            int id_cliente = Integer.parseInt(jO.get("id_cliente").toString());

            Calendar c = new GregorianCalendar();
            String fechaSolicitud = c.get(GregorianCalendar.DAY_OF_MONTH) + "-"
                    + (c.get(GregorianCalendar.MONTH) + 1) + "-" + c.get(GregorianCalendar.YEAR);

            String fechaEntrega = c.get(GregorianCalendar.DAY_OF_MONTH) + "-"
                    + (c.get(GregorianCalendar.MONTH) + 2) + "-" + c.get(GregorianCalendar.YEAR);

            System.out.println("FEcha actual " + fechaSolicitud);
            String sql = "select max (id) as MAXIMO from PEDIDO_PRODUCTO";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            escribirEnLog(sql);
            int id_pedido = 1;
            if (rs.next()) {
                id_pedido = rs.getInt("MAXIMO") + 1;
                jRespuesta.put("id_pedido", id_pedido);

                System.out.println("JSON respuesta " + jRespuesta.toString());
                //Crear pedido nuevo
                sql = "INSERT INTO PEDIDO_PRODUCTO (id,FECHA_ESPERADA_ENTREGA,Estado,cantidad_producto"
                        + ",id_cliente,fecha_solicitud) VALUES (" + id_pedido + ",TO_DATE"
                        + "('" + sFecha + "','DD-MM-YYYY'),'Espera'," + cantidad + ","
                        + id_cliente + " ,TO_DATE('" + fechaSolicitud + "','DD-MM-YYYY'))";

                System.out.println("----------------------Query-----------------------");
                System.out.println(sql);
                Statement st2 = con.createStatement();

                st2.executeUpdate(sql);

                escribirEnLog(sql);
                st2.close();
            }
            st.close();

            int productosReservados = reservarProductoBodega(nombreProducto, cantidad, id_pedido);

            System.out.println("Productos reservados " + productosReservados);
            if (productosReservados == cantidad) {

                //Modificar fecha entrega
                Statement st3 = con.createStatement();
                sql = "update PEDIDO_PRODUCTO set FECHA_ENTREGA=TO_DATE('" + sFecha + "','DD-MM-YYYY'),"
                        + "ESTADO='En Bodega'"
                        + "where id=" + id_pedido;
                System.out.println("------------------QUERY----------------------------");
                System.out.println(sql);
                st3.executeUpdate(sql);

                escribirEnLog(sql);
                st3.close();
                resp = "En Bodega";
            } else {

                // Verificar que la cantidad disminuye dependiendo de cuantos productos ya están en bodega
                cantidad = cantidad - productosReservados;

                //Verificar que haya estación de produccion disponible
                sql = "select * from ESTACION where ESTADO='Disponible' AND CAPACIDAD > " + cantidad;
                System.out.println("------------------QUERY----------------------------");
                System.out.println(sql);

                Statement st6 = con.createStatement();
                rs = st6.executeQuery(sql);

                boolean hayEstaciones = false;

                if (rs.next()) {

                    int codigo = rs.getInt("CODIGO");

                    sql = "update ESTACION set ESTADO='Reservado',ID_PEDIDO=" + id_pedido
                            + "WHERE CODIGO=" + codigo;
                    Statement st7 = con.createStatement();
                    st7.executeUpdate(sql);
                    hayEstaciones = true;
                    st7.close();
                }

                st6.close();

                if (hayEstaciones) {
                    //Reservar recursos(materias primas) o pedir suministros
                    int numProductosPotencial = Integer.MAX_VALUE;

                    System.out.println("Nombre producto: " + nombreProducto);
                    //Averiguar Componentes en bodega
                    sql = "select * from COMPONENTES_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                    Statement st3 = con.createStatement();
                    rs = st3.executeQuery(sql);

                    while (rs.next()) {

                        String id_componente = rs.getString("id_componente");
                        int cantidad_unidades = rs.getInt("cantidad_unidades");

                        int numComponentes = cantidadComponentesBodega(id_componente);
                        if (numComponentes >= cantidad_unidades) {

                            int alcanzanComponentes = numComponentes / cantidad_unidades;
                            numProductosPotencial = Math.min(alcanzanComponentes, numProductosPotencial);
                        }
                    }

                    st3.close();

                    //Averiguar Materias Primas en bodega
                    sql = "select * from MATERIAS_PRIMAS_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                    st3 = con.createStatement();
                    rs = st3.executeQuery(sql);

                    while (rs.next()) {

                        String id_materia = rs.getString("id_materia_prima");
                        int cantidad_unidades = rs.getInt("cantidad_unidades");

                        int numMateriasBodega = cantidadMateriasPrimasBodega(id_materia);
                        if (numMateriasBodega >= cantidad_unidades) {

                            int alcanzanMaterias = numMateriasBodega / cantidad_unidades;
                            numProductosPotencial = Math.min(alcanzanMaterias, numProductosPotencial);
                        }
                    }

                    st3.close();

                    System.out.println("Numero productos se pueden hacer con bodega " + numProductosPotencial);

                    if (numProductosPotencial != Integer.MAX_VALUE
                            && numProductosPotencial >= cantidad) {

                        //Reservar componentes
                        sql = "select * from COMPONENTES_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                        st3 = con.createStatement();
                        rs = st3.executeQuery(sql);

                        while (rs.next()) {

                            String id_componente = rs.getString("id_componente");
                            int cantidad_unidades = rs.getInt("cantidad_unidades");
                            reservarComponenteBodega(id_componente, cantidad * cantidad_unidades, id_pedido);
                        }
                        st3.close();

                        // Reservar materias primas
                        sql = "select * from MATERIAS_PRIMAS_PRODUCTO WHERE id_producto='" + nombreProducto + "'";
                        st3 = con.createStatement();
                        rs = st3.executeQuery(sql);

                        while (rs.next()) {

                            String id_materia = rs.getString("id_materia_prima");
                            int cantidad_unidades = rs.getInt("cantidad_unidades");
                            reservarMateriaPrimaBodega(id_materia, cantidad * cantidad_unidades, id_pedido);
                        }

                        st3.close();

                        //Falta fecha esperada
                        crearItemsReservadosPedido(nombreProducto, id_pedido, cantidad);
                        resp = "Espera";
                    } else {
                        //Poner el pedido en estad ESPERA
                        st3 = con.createStatement();
                        sql = "update PEDIDO_PRODUCTO set ESTADO='Espera' where id=" + id_pedido;
                        st3.executeUpdate(sql);
                        st3.close();

                        resp = "Espera";
                    }
                } else {
                    resp = "Espera";
                }
            }
            cerrarConexion();
            //return resp;

            jRespuesta.put("Respuesta", resp);
            return jRespuesta;
        } catch (Exception e) {
            e.printStackTrace();
            rollback();
            //return "error";
            JSONObject jRespuesta = new JSONObject();
            jRespuesta.put("Respuesta", "error");
            return jRespuesta;
        }
    }

    @GET
    @Path("/darEtapasCuentaJose")
    public String darEtapasCuentaJose() throws Exception {

        JSONArray jArray = new JSONArray();
        abrirConexion();
        String sql = "select ESTACION_ID, count(*) as num_etapas from ETAPA_ESTACION group by ESTACION_ID";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(sql);
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            JSONObject jObject = new JSONObject();
            jObject.put("estacion_id", rs.getInt("estacion_id"));
            jObject.put("num_etapas", rs.getInt("num_etapas"));
            System.out.println(jObject);
            jArray.add(jObject);
        }
        System.out.println(jArray);
        st.close();
        cerrarConexion();
        JSONObject jObject = new JSONObject();
        jObject.put("arreglo", jArray);
        return "pj-r::" + jObject;
    }

    @GET
    @Path("/borrarRegEtapaEstacionJose/{id_etapa}/{id_estacion}")
    public String borrarRegEtapaEstacionJose(@PathParam("id_etapa") int etapaId, @PathParam("id_estacion") int estacionId) throws Exception {
        abrirConexion();
        String sql = "DELETE FROM ETAPA_ESTACION WHERE ETAPA_ID=" + etapaId + " AND ESTACION_ID=" + estacionId;
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(sql);
        Statement st = con.createStatement();
        st.executeQuery(sql);
        st.close();
        cerrarConexion();
        return "Bien";
    }

    @GET
    @Path("/insertarRegEtapaEstacionJose/{id_etapa}/{id_estacion}")
    public String insertarRegEtapaEstacionJose(@PathParam("id_etapa") int etapaId, @PathParam("id_estacion") int estacionId) throws Exception {
        abrirConexion();
        String sql = "INSERT INTO ETAPA_ESTACION (ETAPA_ID,ESTACION_ID) VALUES (" + etapaId + "," + estacionId + ")";
        System.out.println("- - - - - - - - - - - - - - - - - Print Query - - - - - - - - - - - - - - - - -");
        System.out.println(sql);
        Statement st = con.createStatement();
        st.executeQuery(sql);
        st.close();
        cerrarConexion();
        return "Bien";
    }

    @GET
    @Path("/metodoPrueba")
    public String metodoPrueba() {
        try {
            Send env = new Send();
            env.enviar("pj-pe");
            env.close();
            System.out.println("Bien");

            org.json.JSONObject jRespuesta;
            Long milis = System.currentTimeMillis();
            org.json.JSONArray jArray = new org.json.JSONArray();
            while (System.currentTimeMillis() - milis < 10000) {
                for (int i = 0; i < buzon.size(); i++) {
                    if (buzon.get(i).startsWith("jp-r")) {
                        String[] arregloTexto = buzon.get(i).split("::");
                        String texto = arregloTexto[1];
                        jRespuesta = new org.json.JSONObject(texto);
                        jArray = jRespuesta.getJSONArray("arreglo");
                    }
                }
            }
            return "Retorna " + jArray.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return ("Mal");
        }
    }

    public String consultarEtapasRangoFechaRFC8y9AEnviar(JSONObject jP) throws Exception {
        try {
            abrirConexion();
            String sReturn = "";

            String igualdad = (jP.get("igualdad").toString().equals("SI")) ? "=" : "!=";
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = format.parse(jP.get("fecha1").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            Calendar cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fecha1 = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1) + "-"
                    + cEsp.get(GregorianCalendar.YEAR);
            String fecha1Env = (cEsp.get(GregorianCalendar.MONTH) + 1) + "/" + (cEsp.get(GregorianCalendar.DAY_OF_MONTH)) + "/"
                    + cEsp.get(GregorianCalendar.YEAR);

            date = format.parse(jP.get("fecha2").toString().substring(0, 10));
            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            cEsp = new GregorianCalendar();
            cEsp.setTime(date);

            String fecha2 = cEsp.get(GregorianCalendar.DAY_OF_MONTH) + "-" + (cEsp.get(GregorianCalendar.MONTH) + 1) + "-"
                    + cEsp.get(GregorianCalendar.YEAR);
            String fecha2Env = (cEsp.get(GregorianCalendar.MONTH) + 1) + "/" + (cEsp.get(GregorianCalendar.DAY_OF_MONTH)) + "/"
                    + cEsp.get(GregorianCalendar.YEAR);

            String criterio = jP.get("criterio").toString();
            String valor = jP.get("valor").toString();

            // RC12-solicitud-id-fechaInicial-FechaFinal
            if (criterio.equals("materia prima")) {

                Statement st = con.createStatement();
                String sql = "select* from "
                        + "(select * from materias_primas_producto where id_materia_prima" + igualdad + "'" + valor + "') "
                        + " inner join "
                        + " (select * from "
                        + "  (select codigo_secuencia from ETAPA_FECHA where FECHA>=TO_DATE('" + fecha1 + "','dd-mm-yyyy') "
                        + "AND FECHA<=TO_DATE('" + fecha2 + "','dd-mm-yyyy') "
                        + "  group by CODIGO_SECUENCIA)c  "
                        + "   inner join "
                        + "   ETAPA_DE_PRODUCCION "
                        + "   on ETAPA_DE_PRODUCCION.numero_secuencia=c.codigo_secuencia) "
                        + "  on "
                        + "  id_producto = nombre_producto";
                System.out.println("RFC8 ----------- QUERY\n" + sql);

                ResultSet rs = st.executeQuery(sql);

                sReturn += "id_producto-id_etapa-descripcion-numero_etapa-materia_prima";
                while (rs.next()) {

                    sReturn += "/" + rs.getString("id_producto") + "-" + rs.getInt("numero_secuencia") + "-" + rs.getString("descripcion") + "-"
                            + rs.getInt("etapa") + "-" + rs.getString("id_materia_prima");
                }
            } else if (criterio.equals("tipo material")) {
                Statement st = con.createStatement();
                String sql = "select* from      \n"
                        + "      (select id_producto,tipo  from MATERIA_PRIMA inner join MATERIAS_PRIMAS_PRODUCTO on "
                        + "NOMBRE=ID_MATERIA_PRIMA \n"
                        + "                  where tipo" + igualdad + "'" + valor + "' \n"
                        + "                  )\n"
                        + "      inner join \n"
                        + "      (select * from \n"
                        + "            (select codigo_secuencia from ETAPA_FECHA where FECHA>=TO_DATE('" + fecha1 + "','dd-mm-yyyy') "
                        + "AND FECHA<=TO_DATE('" + fecha2 + "','dd-mm-yyyy') \n"
                        + "                      group by CODIGO_SECUENCIA)c \n"
                        + "            inner join\n"
                        + "            ETAPA_DE_PRODUCCION\n"
                        + "            on ETAPA_DE_PRODUCCION.numero_secuencia=c.codigo_secuencia)\n"
                        + "      on\n"
                        + "      id_producto = nombre_producto";
                System.out.println("RFC8 ----------- QUERY\n" + sql);

                ResultSet rs = st.executeQuery(sql);
                sReturn += "id_producto-id_etapa-descripcion-numero_etapa-tipo_material";
                while (rs.next()) {

                    sReturn += "/" + rs.getString("id_producto") + "-" + rs.getInt("numero_secuencia") + "-" + rs.getString("descripcion") + "-"
                            + rs.getInt("etapa") + "-" + rs.getString("tipo");
                }

            } else if (criterio.equals("pedido")) {
                Statement st = con.createStatement();
                String sql = "select* from      \n"
                        + "      (select id_producto,id_pedido from ITEM inner join MATERIAS_PRIMAS_PRODUCTO on "
                        + "ITEM.NOMBRE_PRODUCTO=MATERIAS_PRIMAS_PRODUCTO.ID_PRODUCTO\n"
                        + "        where ITEM.ID_PEDIDO" + igualdad + valor + "\n"
                        + "                  )\n"
                        + "      inner join \n"
                        + "      (select * from \n"
                        + "            (select codigo_secuencia from ETAPA_FECHA where FECHA>=TO_DATE('" + fecha1 + "','dd-mm-yyyy') "
                        + "AND FECHA<=TO_DATE('" + fecha2 + "','dd-mm-yyyy') \n"
                        + "                      group by CODIGO_SECUENCIA)c \n"
                        + "            inner join\n"
                        + "            ETAPA_DE_PRODUCCION\n"
                        + "            on ETAPA_DE_PRODUCCION.numero_secuencia=c.codigo_secuencia)\n"
                        + "      on\n"
                        + "      id_producto = nombre_producto";
                System.out.println("RFC8 ----------- QUERY\n" + sql);

                ResultSet rs = st.executeQuery(sql);

                sReturn += "id_producto-id_etapa-descripcion-numero_etapa-id_pedido";
                while (rs.next()) {
//                    JSONObject jO = new JSONObject();
//                    jO.put("id_producto", rs.getString("id_producto"));
//                    jO.put("id_etapa", rs.getInt("numero_secuencia"));
//                    jO.put("descripcion", rs.getString("descripcion"));
//                    jO.put("numero_etapa", rs.getInt("etapa"));
//                    jO.put("id_pedido", rs.getInt("id_pedido"));
//                    jResp.add(jO);
                    sReturn += "/" + rs.getString("id_producto") + "-" + rs.getInt("numero_secuencia") + "-" + rs.getString("descripcion") + "-"
                            + rs.getInt("etapa") + "-" + rs.getString("id_pedido");
                }

            }

            cerrarConexion();

            return sReturn;
        } catch (Exception e) {
            rollback();
            throw e;
        }

    }
}

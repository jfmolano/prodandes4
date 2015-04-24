/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package prodandes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Jonathan
 */
@Path("/ServiciosMock")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ServiciosMock {

    @POST
    @Path("/registrarPedido")
    public JSONObject registrarPedido(JSONObject jO) throws Exception {

        System.out.println("jO " + jO.toJSONString());
        System.out.println("jO " + jO.toString());
        String nomProducto = jO.get("nombre").toString();
        int id_cliente = (int) jO.get("id_cliente");
        int cantidad = (int) jO.get("cantidad");
        String fecha = jO.get("fechaEsperada").toString();

        JSONObject jr = new JSONObject();
        jr.put("Respuesta", "Hola");
        return jr;
    }

    @POST
    @Path("/registrarEntregaPedidoProductosCliente")
    public void registrarEntregaPedidoProductosCliente(JSONObject jO) {

        System.out.println(jO.get("id_pedido"));
    }

    @POST
    @Path("/consultarProductos")
    public JSONArray consultarProductos(JSONObject jO) throws Exception{

        String criterio = jO.get("Criterio").toString();

        if (criterio.equals("Fecha solicitud")) {
            
            
//
//            System.out.println("FEcha mock " + jO.get("fecha_solicitud").toString());
//            
//            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//            Date date = format.parse(jO.get("fecha_solicitud").toString().substring(0, 10));
//            System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
            
        }
        JSONArray jA = new JSONArray();

        for (int i = 0; i < 10; i++) {
            JSONObject jObject = new JSONObject();
            jObject.put("id", i);
            jObject.put("ESTADO", "estado " + i);
            jObject.put("NOMBRE_PRODUCTO", "nombre " + i);
            jObject.put("ETAPA", "etapa " + i);
            jObject.put("ID_PEDIDO", i);
            jA.add(jObject);

        }
        return jA;
    }
    
    @POST
    @Path("/consultarMateriasPrimas")
    public JSONArray consultarMateriasPrimas(JSONObject jOx) throws Exception{

        String criterio = jOx.get("Criterio").toString();

        JSONArray jA = new JSONArray();

        for (int i = 0; i < 10; i++) {
            JSONObject jObject = new JSONObject();
            jObject.put("id", i);
            jObject.put("ESTADO", "estado " + i);
            jObject.put("Materia", "materia " + i);
            jObject.put("ID_PEDIDO", i);
            jA.add(jObject);           

        }
        return jA;
    }
    
    @POST
    @Path("/consultarComponentes")
    public JSONArray consultarComponentes(JSONObject jOx) throws Exception{

        String criterio = jOx.get("Criterio").toString();

        JSONArray jA = new JSONArray();

        for (int i = 0; i < 10; i++) {
            JSONObject jObject = new JSONObject();
            jObject.put("id", i);
            jObject.put("ESTADO", "estado " + i);
            jObject.put("componente", "componente " + i);
            jObject.put("ID_PEDIDO", i);
            jA.add(jObject);           

        }
        return jA;
    }
    
        @POST
    @Path("/registrarProveedor")
    public JSONObject registrarProveedor(JSONObject jO) throws Exception {

        System.out.println("jO " + jO.toJSONString());
        System.out.println("jO " + jO.toString());
        JSONObject jr = new JSONObject();
        jr.put("Respuesta", "Hola");
        return jr;
    }
    
        @POST
    @Path("/registrarEjecucionEtapa")
    public JSONObject registrarEjecucionEtapa(JSONObject jO) throws Exception {

        System.out.println("jO " + jO.toJSONString());
        System.out.println("jO " + jO.toString());
        JSONObject jr = new JSONObject();
        jr.put("Respuesta", "Hola");
        return jr;
    }
    
        @POST
    @Path("/llegadaComponente")
    public JSONObject llegadaComponente(JSONObject jO) throws Exception {

        System.out.println("jO " + jO.toJSONString());
        System.out.println("jO " + jO.toString());
        JSONObject jr = new JSONObject();
        jr.put("Respuesta", "Hola");
        return jr;
    }
    
        @POST
    @Path("/llegadaMaterial")
    public JSONObject llegadaMaterial(JSONObject jO) throws Exception {

        System.out.println("jO " + jO.toJSONString());
        System.out.println("jO " + jO.toString());
        JSONObject jr = new JSONObject();
        jr.put("Respuesta", "Hola");
        return jr;
    }
}

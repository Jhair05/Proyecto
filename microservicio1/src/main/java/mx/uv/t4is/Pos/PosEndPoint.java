package mx.uv.t4is.Pos;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import https.t4is_uv_mx.pos.VentaRequest;
import https.t4is_uv_mx.pos.VentaResponse;
import https.t4is_uv_mx.pos.ModificarStockRequest;
import https.t4is_uv_mx.pos.ModificarStockResponse;

@Endpoint
public class PosEndPoint {
    @Autowired
    private IPOS ipos;

    @Autowired
    private Iinventory iinventory;

    @Autowired
    private IUsuario iusuario;

    @Autowired
    private IStock istock;

    // hacer venta
    @PayloadRoot(namespace = "https://t4is.uv.mx/pos", localPart = "VentaRequest")
    @ResponsePayload
    public VentaResponse vender(@RequestPayload VentaRequest peticion) {
        VentaResponse respuesta = new VentaResponse();
        Venta venta = new Venta();

        try {
            // Verificar si están vacios los parametros
            if (peticion.getVendedor().isEmpty() || peticion.getArticulo() == 0 || peticion.getCantidad() == 0) {
                respuesta.setMsg("No dejes campos vacios");
                return respuesta;
            }

            // Obtener item (Se usa optional porque no sabemos si va a estar)
            Optional<Inventario> item = iinventory.findById(peticion.getArticulo());
            // En caso de no estar el articulo en la base
            if (!item.isPresent()) {
                respuesta.setMsg("Producto no encontrado");
                return respuesta;
            }

            System.out.println(item.get().getNombre());
            System.out.println(item.get().getCantidad());
            
            // Si no hay stock no se registra la venta
            if (item.get().getCantidad() == 0) {
                respuesta.setMsg("No hay stock, no se realizó la venta");
                return respuesta;
            }

            // Si la cantidad de producto a vender supera la que hay en stock
            if (peticion.getCantidad() > item.get().getCantidad()) {
                respuesta.setMsg("La venta supera el limite de stock");
                return respuesta;
            }

            // Actualizar stock restandole a la cantidad del item en stock, lo que se va a vender
            item.get().setCantidad(item.get().getCantidad() - peticion.getCantidad());
            iinventory.save(item.get());

            // Guardar parametros en objeto
            venta.setArticulo(item.get());
            venta.setCantidad(peticion.getCantidad());
            venta.setVendedor(peticion.getVendedor());
            
            // Guardar venta
            ipos.save(venta);
            // setteando mensaje en respuesta
            respuesta.setMsg("guardados correctamente");
        } catch (Exception e) {
            respuesta.setMsg("Algo salió muy mal :(");
            return respuesta;
        }

        return respuesta;
    }

    // Actualizar stock
    @PayloadRoot(namespace = "https://t4is.uv.mx/pos", localPart = "ModificarStockRequest")
    @ResponsePayload
    public ModificarStockResponse modificarContrato(@RequestPayload ModificarStockRequest peticion) {
        ModificarStockResponse respuesta = new ModificarStockResponse();

        try {
            // Verificar si están vacios los parametros o si ambos stock están en 0
            if (peticion.getNickname().isEmpty() || (peticion.getTortillas() == 0 && peticion.getTotopos() == 0)) {
                respuesta.setMsg("Error: No nickname o ambos stocks están vacios");
                return respuesta;
            }

            // Obtener el usuario (Se usa optional porque no sabemos si va a estar)
            Optional<Usuario> usuario = iusuario.findByNickname(peticion.getNickname());
            // En caso de no estar el usuario en la base de datos
            if (!usuario.isPresent()) {
                respuesta.setMsg("Usuario no encontrado");
                return respuesta;
            }

            // Actualizar cantidades
            Stock newStock = new Stock();
            newStock.setId(usuario.get().getId());
            newStock.setUsuario(usuario.get());
            newStock.setTortillas(peticion.getTortillas());
            newStock.setTotopos(peticion.getTotopos());

            // Actualizar stock
            istock.save(newStock);
        } catch (Exception e) {
            System.out.println(e);
            respuesta.setMsg("Algo salió muy mal :(");
            return respuesta;
        }

        respuesta.setMsg("Se actualizó correctamente el stock");
        return respuesta;
    }
}
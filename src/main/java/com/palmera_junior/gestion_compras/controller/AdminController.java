package com.palmera_junior.gestion_compras.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.palmera_junior.gestion_compras.entity.CentroCosto;
import com.palmera_junior.gestion_compras.entity.PresentacionProducto;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Categoria;
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.service.CentroCostoService;
import com.palmera_junior.gestion_compras.service.ProductoService;
import com.palmera_junior.gestion_compras.service.ProveedorService;
import com.palmera_junior.gestion_compras.service.SedeService;
import com.palmera_junior.gestion_compras.service.UsuarioService;

@Controller
public class AdminController {

    private final ProveedorService proveedorService;
    private final UsuarioService usuarioService;
    private final ProductoService productoService;
    private final SedeService sedeService;
    private final CentroCostoService centroCostoService;

    public AdminController(ProveedorService proveedorService, UsuarioService usuarioService,
            ProductoService productoService, SedeService sedeService, CentroCostoService centroCostoService) {
        this.proveedorService = proveedorService;
        this.usuarioService = usuarioService;
        this.productoService = productoService;
        this.sedeService = sedeService;
        this.centroCostoService = centroCostoService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarPanelAdministracion(Model model,
            @RequestParam(defaultValue = "0") int pageProveedores,
            @RequestParam(defaultValue = "0") int pageUsuarios,
            @RequestParam(defaultValue = "0") int pageProductos,
            @RequestParam(defaultValue = "0") int pageSedes,
            @RequestParam(defaultValue = "0") int pageCentros,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String error) {

        List<Proveedor> proveedores = proveedorService.listarTodos();
        List<Usuario> usuarios = usuarioService.listarTodos();
        List<Producto> productos = productoService.getAllProductos();
        List<Sede> sedes = sedeService.listarTodos();
        List<CentroCosto> centroCostos = centroCostoService.getAllCentroCostos();

        Page<Proveedor> proveedoresPage = proveedorService.paginar(pageProveedores, size);
        Page<Usuario> usuariosPage = usuarioService.paginar(pageUsuarios, size);
        Page<Producto> productosPage = productoService.paginar(pageProductos, size);
        Page<Sede> sedesPage = sedeService.paginar(pageSedes, size);
        Page<CentroCosto> centrosCostoPage = centroCostoService.paginar(pageCentros, size);

        model.addAttribute("proveedoresPage", proveedoresPage);
        model.addAttribute("usuariosPage", usuariosPage);
        model.addAttribute("productosPage", productosPage);
        model.addAttribute("sedesPage", sedesPage);
        model.addAttribute("centrosCostoPage", centrosCostoPage);

        model.addAttribute("proveedores", proveedores);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("productos", productos);
        model.addAttribute("sedes", sedes);
        model.addAttribute("centrosCosto", centroCostos);
        model.addAttribute("pageProveedores", pageProveedores);
        model.addAttribute("pageUsuarios", pageUsuarios);
        model.addAttribute("pageProductos", pageProductos);
        model.addAttribute("pageSedes", pageSedes);
        model.addAttribute("pageCentros", pageCentros);
        model.addAttribute("size", size);
        model.addAttribute("roles", Arrays.asList(Rol.values()));
        model.addAttribute("categorias", Arrays.asList(Categoria.values()));
        model.addAttribute("successMessage", success);
        model.addAttribute("errorMessage", error);

        return "admin";
    }

    // Endpoint AJAX para paginar la tabla de productos sin recargar la página
    @GetMapping("/admin/productos/pagina")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String paginaProductos(Model model,
            @RequestParam(defaultValue = "0") int pageProductos,
            @RequestParam(defaultValue = "10") int size) {

        Page<Producto> productosPage = productoService.paginar(pageProductos, size);

        model.addAttribute("productosPage", productosPage);
        model.addAttribute("pageProductos", pageProductos);
        model.addAttribute("size", size);

        return "admin :: productosFragment";
    }

    // Endpoint AJAX para paginar la tabla de Proveedores sin recargar la página
    @GetMapping("/admin/proveedores/pagina")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String paginaProveedores(Model model,
            @RequestParam(defaultValue = "0") int pageProveedores,
            @RequestParam(defaultValue = "10") int size) {
        Page<Proveedor> proveedoresPage = proveedorService.paginar(pageProveedores, size);
        model.addAttribute("proveedoresPage", proveedoresPage);
        model.addAttribute("pageProveedores", pageProveedores);
        model.addAttribute("size", size);
        return "admin :: proveedoresFragment";
    }

    // Endpoint AJAX para paginar la tabla de Usuarios sin recargar la página
    @GetMapping("/admin/usuarios/pagina")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String paginaUsuarios(Model model,
            @RequestParam(defaultValue = "0") int pageUsuarios,
            @RequestParam(defaultValue = "10") int size) {
        Page<Usuario> usuariosPage = usuarioService.paginar(pageUsuarios, size);
        model.addAttribute("usuariosPage", usuariosPage);
        model.addAttribute("pageUsuarios", pageUsuarios);
        model.addAttribute("size", size);
        return "admin :: usuariosFragment";
    }

    @PostMapping("/admin/proveedores")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarProveedor(@RequestParam(required = false) Integer idProv,
            @RequestParam String nit,
            @RequestParam String nombre,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) List<Integer> sedeIds,
            RedirectAttributes redirectAttributes) {

        try {
            proveedorService.guardar(idProv, nit, nombre, ciudad, direccion, telefono, correo, sedeIds);
            redirectAttributes.addAttribute("success",
                    idProv == null ? "Proveedor creado correctamente." : "Proveedor actualizado correctamente.");
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin";
        }
    }

    @PostMapping("/admin/usuarios")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarUsuario(@RequestParam(required = false) Integer idUsuario,
            @RequestParam String cedula,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam(required = false) String cargo,
            @RequestParam String nombreUsuario,
            @RequestParam(required = false) String contrasena,
            @RequestParam String email,
            @RequestParam Rol rol,
            @RequestParam Integer sedeId,
            RedirectAttributes redirectAttributes) {

        try {
            usuarioService.guardarUsuario(idUsuario, cedula, nombre, apellido, cargo, nombreUsuario, contrasena,
                    email, rol, sedeId);
            redirectAttributes.addAttribute("success",
                    idUsuario == null ? "Usuario creado correctamente." : "Usuario actualizado correctamente.");
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin";
        }
    }

@PostMapping("/admin/productos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public String guardarProducto(@RequestParam(required = false) Integer idProducto,
        @RequestParam String codigoInventario,
        @RequestParam String nombre,
        @RequestParam(required = false) String categoria,
        RedirectAttributes redirectAttributes,
        @RequestParam(required = false) List<String> presentacionNombres,
        @RequestParam(required = false) List<Integer> presentacionCantidades,
        @RequestParam(required = false) List<String> presentacionUnidades,
        @RequestParam(required = false) List<java.math.BigDecimal> presentacionPrecios) {

    try {
        productoService.guardarProducto(idProducto, codigoInventario, nombre, categoria, presentacionNombres,
                presentacionCantidades, presentacionUnidades, presentacionPrecios);
        redirectAttributes.addAttribute("success",
                idProducto == null ? "Producto creado correctamente." : "Producto actualizado correctamente.");
        return "redirect:/admin";
    } catch (IllegalArgumentException ex) {
        redirectAttributes.addAttribute("error", ex.getMessage());
        return "redirect:/admin";
    } catch (DataAccessException e) {
        String causa = e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();
        redirectAttributes.addAttribute("error",
                "No se pudo guardar el producto. Detalle: " + causa);
        return "redirect:/admin";
    } catch (Exception e) {
        redirectAttributes.addAttribute("error",
                "Ocurrió un error inesperado al guardar el producto: " + e.getMessage());
        return "redirect:/admin";
    }
}

    @PostMapping("/admin/sedes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarSede(@RequestParam(required = false) Integer idSede,
            @RequestParam String nombre,
            @RequestParam String prefijoCiudad,
            @RequestParam(required = false) String direccion,
            RedirectAttributes redirectAttributes) {

        try {
            sedeService.guardar(idSede, nombre, prefijoCiudad, direccion);
            redirectAttributes.addAttribute("success",
                    idSede == null ? "Sede creada correctamente." : "Sede actualizada correctamente.");
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin";
        }
    }

    @PostMapping("/admin/centros-costo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarCentroCosto(@RequestParam(required = false) Integer idCentroCosto,
            @RequestParam String nombre,
            @RequestParam Integer sedeId,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String direccion,
            RedirectAttributes redirectAttributes) {

        try {
            centroCostoService.guardar(idCentroCosto, nombre, sedeId, codigo, direccion);
            redirectAttributes.addAttribute("success",
                    idCentroCosto == null ? "Centro de costo creado correctamente." : "Centro de costo actualizado correctamente.");
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin";
        }
    }

    @PostMapping("/admin/productos/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteProducto(@PathVariable Integer id) {
        if (!productoService.eliminarProducto(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Producto no encontrado"));
        }
        return ResponseEntity.ok(Map.of("success", "Producto eliminado"));
    }

    // Endpoint para cargar las presentaciones de un producto al editar en admin
    @GetMapping("/admin/producto/{id}/presentaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseBody
    public List<PresentacionProducto> obtenerPresentacionesProducto(@PathVariable Integer id) {
        return productoService.obtenerPresentacionesProducto(id);
    }

    @PostMapping("/admin/centros-costo/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteCentro(@PathVariable Integer id) {
        if (!centroCostoService.eliminar(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Centro no encontrado"));
        }
        return ResponseEntity.ok(Map.of("success", "Centro eliminado"));
    }

    @PostMapping("/admin/usuarios/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteUsuario(@PathVariable Integer id) {
        if (!usuarioService.eliminar(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
        }
        return ResponseEntity.ok(Map.of("success", "Usuario eliminado"));
    }

    @PostMapping("/admin/proveedores/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteProveedor(@PathVariable Integer id) {
        if (!proveedorService.eliminar(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Proveedor no encontrado"));
        }
        return ResponseEntity.ok(Map.of("success", "Proveedor eliminado"));
    }

    @PostMapping("/admin/sedes/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteSede(@PathVariable Integer id) {
        if (!sedeService.eliminar(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sede no encontrada"));
        }
        return ResponseEntity.ok(Map.of("success", "Sede eliminada"));
    }
}

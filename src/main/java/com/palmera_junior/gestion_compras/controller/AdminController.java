package com.palmera_junior.gestion_compras.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import java.util.stream.Collectors;

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
            @RequestParam(required = false) String searchProveedores,
            @RequestParam(required = false) String searchUsuarios,
            @RequestParam(required = false) String rolUsuario,
            @RequestParam(required = false) Integer sedeIdUsuario,
            @RequestParam(required = false) String searchProductos,
            @RequestParam(required = false) String categoriaProducto,
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String error) {

        List<Proveedor> proveedores = proveedorService.listarTodos();
        List<Usuario> usuarios = usuarioService.listarTodos();
        List<Producto> productos = productoService.getAllProductos();
        List<Sede> sedes = sedeService.listarTodos();
        List<CentroCosto> centroCostos = centroCostoService.getAllCentroCostos();

        // Aplicar filtros en memoria (suficiente para datasets pequeños y evita cambios en repositorios)
        List<Proveedor> proveedoresFiltrados = proveedores.stream().filter(p -> {
            if (searchProveedores == null || searchProveedores.trim().isEmpty()) return true;
            String s = searchProveedores.trim().toLowerCase();
            return (p.getNombre() != null && p.getNombre().toLowerCase().contains(s))
                    || (p.getNit() != null && p.getNit().toLowerCase().contains(s));
        }).collect(Collectors.toList());

        List<Usuario> usuariosFiltrados = usuarios.stream().filter(u -> {
            boolean ok = true;
            if (searchUsuarios != null && !searchUsuarios.trim().isEmpty()) {
                String s = searchUsuarios.trim().toLowerCase();
                String nombreCompleto = (u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "");
                ok = (nombreCompleto.toLowerCase().contains(s))
                        || (u.getCedula() != null && u.getCedula().toLowerCase().contains(s))
                        || (u.getNombreUsuario() != null && u.getNombreUsuario().toLowerCase().contains(s));
            }
            if (ok && rolUsuario != null && !rolUsuario.trim().isEmpty()) {
                ok = u.getRol() != null && u.getRol().toString().equalsIgnoreCase(rolUsuario);
            }
            if (ok && sedeIdUsuario != null) {
                ok = u.getSede() != null && u.getSede().getIdSede().equals(sedeIdUsuario);
            }
            return ok;
        }).collect(Collectors.toList());

        List<Producto> productosFiltrados = productos.stream().filter(p -> {
            boolean ok = true;
            if (searchProductos != null && !searchProductos.trim().isEmpty()) {
                String s = searchProductos.trim().toLowerCase();
                ok = (p.getNombre() != null && p.getNombre().toLowerCase().contains(s))
                        || (p.getCodigoInventario() != null && p.getCodigoInventario().toLowerCase().contains(s));
            }
            if (ok && categoriaProducto != null && !categoriaProducto.trim().isEmpty()) {
                ok = p.getCategoria() != null && p.getCategoria().toString().equalsIgnoreCase(categoriaProducto);
            }
            return ok;
        }).collect(Collectors.toList());

        // Crear pages manualmente para respetar paginación
        Page<Proveedor> proveedoresPage;
        {
            int total = proveedoresFiltrados.size();
            int start = Math.min(pageProveedores * size, total);
            int end = Math.min(start + size, total);
            List<Proveedor> content = (start < end) ? proveedoresFiltrados.subList(start, end) : java.util.Collections.emptyList();
            proveedoresPage = new PageImpl<>(content, PageRequest.of(pageProveedores, size), total);
        }

        Page<Usuario> usuariosPage;
        {
            int total = usuariosFiltrados.size();
            int start = Math.min(pageUsuarios * size, total);
            int end = Math.min(start + size, total);
            List<Usuario> content = (start < end) ? usuariosFiltrados.subList(start, end) : java.util.Collections.emptyList();
            usuariosPage = new PageImpl<>(content, PageRequest.of(pageUsuarios, size), total);
        }

        Page<Producto> productosPage;
        {
            int total = productosFiltrados.size();
            int start = Math.min(pageProductos * size, total);
            int end = Math.min(start + size, total);
            List<Producto> content = (start < end) ? productosFiltrados.subList(start, end) : java.util.Collections.emptyList();
            productosPage = new PageImpl<>(content, PageRequest.of(pageProductos, size), total);
        }

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
        model.addAttribute("categoriasUnicas", Arrays.asList(Categoria.values()));

        // Mantener valores de filtros en la vista
        model.addAttribute("searchProveedores", searchProveedores);
        model.addAttribute("searchUsuarios", searchUsuarios);
        model.addAttribute("rolUsuario", rolUsuario);
        model.addAttribute("sedeIdUsuario", sedeIdUsuario);
        model.addAttribute("searchProductos", searchProductos);
        model.addAttribute("categoriaProducto", categoriaProducto);

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
        try {
            if (!productoService.eliminarProducto(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Producto no encontrado"));
            }
            return ResponseEntity.ok(Map.of("success", "Producto eliminado"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar el producto porque está asociado a órdenes u otros registros."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar producto: " + dae.getMostSpecificCause()));
        }
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
        try {
            if (!centroCostoService.eliminar(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Centro no encontrado"));
            }
            return ResponseEntity.ok(Map.of("success", "Centro eliminado"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar el centro de costo porque está asociado a órdenes de compra u otros registros."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar centro: " + dae.getMostSpecificCause()));
        }
    }

    @PostMapping("/admin/usuarios/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteUsuario(@PathVariable Integer id) {
        try {
            if (!usuarioService.eliminar(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
            }
            return ResponseEntity.ok(Map.of("success", "Usuario eliminado"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar el usuario porque está asociado a órdenes de compra u otros registros."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar usuario: " + dae.getMostSpecificCause()));
        }
    }

    @PostMapping("/admin/proveedores/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteProveedor(@PathVariable Integer id) {
        try {
            if (!proveedorService.eliminar(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Proveedor no encontrado"));
            }
            return ResponseEntity.ok(Map.of("success", "Proveedor eliminado"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar el proveedor porque está asociado a órdenes de compra u otros registros."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar proveedor: " + dae.getMostSpecificCause()));
        }
    }

    @PostMapping("/admin/sedes/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteSede(@PathVariable Integer id) {
        try {
            if (!sedeService.eliminar(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sede no encontrada"));
            }
            return ResponseEntity.ok(Map.of("success", "Sede eliminada"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar la sede porque tiene centros, órdenes o recursos asociados."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar sede: " + dae.getMostSpecificCause()));
        }
    }
}

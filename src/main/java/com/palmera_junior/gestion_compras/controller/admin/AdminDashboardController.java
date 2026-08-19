package com.palmera_junior.gestion_compras.controller.admin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.palmera_junior.gestion_compras.entity.Categoria;
import com.palmera_junior.gestion_compras.entity.CentroCosto;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.service.catalogo.IProductoService;
import com.palmera_junior.gestion_compras.service.catalogo.IProveedorService;
import com.palmera_junior.gestion_compras.service.organizacion.ICentroCostoService;
import com.palmera_junior.gestion_compras.service.organizacion.ISedeService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

// Único responsable de componer la página inicial del panel de administración,
// agregando en un mismo modelo los cinco agregados administrados (proveedores,
// usuarios, productos, sedes y centros de costo). El CRUD de cada agregado vive
// en su propio controlador (Proveedor/Usuario/Producto/Sede/CentroCosto AdminController).
@Controller
public class AdminDashboardController {

    private final IProveedorService proveedorService;
    private final IUsuarioService usuarioService;
    private final IProductoService productoService;
    private final ISedeService sedeService;
    private final ICentroCostoService centroCostoService;

    public AdminDashboardController(IProveedorService proveedorService, IUsuarioService usuarioService,
            IProductoService productoService, ISedeService sedeService, ICentroCostoService centroCostoService) {
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
}

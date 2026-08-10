package com.palmera_junior.gestion_compras.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.CentroCostoRepository;
import com.palmera_junior.gestion_compras.repository.PresentacionProductoRepository;
import com.palmera_junior.gestion_compras.repository.ProductoRepository;
import com.palmera_junior.gestion_compras.repository.ProveedorRepository;
import com.palmera_junior.gestion_compras.repository.SedeRepository;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;

@Controller
public class AdminController {

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PresentacionProductoRepository presentacionProductoRepository;

    @Autowired
    private SedeRepository sedeRepository;

    @Autowired
    private CentroCostoRepository centroCostoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        // Mantener las listas completas para los selectores y contadores (badges)
        List<Proveedor> proveedores = proveedorRepository.findAll();
        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Producto> productos = productoRepository.findAll();
        List<Sede> sedes = sedeRepository.findAll();
        List<CentroCosto> centroCostos = centroCostoRepository.findAll();

        // Páginas por entidad (cada lista pagina de forma independiente)
        Page<Proveedor> proveedoresPage = proveedorRepository.findAll(PageRequest.of(pageProveedores, size));
        Page<Usuario> usuariosPage = usuarioRepository.findAll(PageRequest.of(pageUsuarios, size));
        Page<Producto> productosPage = productoRepository.findAll(PageRequest.of(pageProductos, size));
        Page<Sede> sedesPage = sedeRepository.findAll(PageRequest.of(pageSedes, size));
        Page<CentroCosto> centrosCostoPage = centroCostoRepository.findAll(PageRequest.of(pageCentros, size));

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

        Page<Producto> productosPage = productoRepository.findAll(PageRequest.of(pageProductos, size));

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
        Page<Proveedor> proveedoresPage = proveedorRepository.findAll(PageRequest.of(pageProveedores, size));
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
        Page<Usuario> usuariosPage = usuarioRepository.findAll(PageRequest.of(pageUsuarios, size));
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

        nit = nit == null ? null : nit.trim();
        nombre = nombre == null ? null : nombre.trim();
        if (nit == null || nit.isBlank() || nombre == null || nombre.isBlank()) {
            redirectAttributes.addAttribute("error", "El NIT y el nombre del proveedor son obligatorios.");
            return "redirect:/admin";
        }

        if (idProv == null) {
            if (proveedorRepository.findByNitIgnoreCase(nit).isPresent()) {
                redirectAttributes.addAttribute("error", "Ya existe un proveedor con ese NIT.");
                return "redirect:/admin";
            }
            Proveedor proveedor = new Proveedor();
            proveedor.setNit(nit);
            proveedor.setNombre(nombre);
            proveedor.setCiudad(ciudad);
            proveedor.setDireccion(direccion);
            proveedor.setTelefono(telefono);
            proveedor.setCorreo(correo);
            if (sedeIds != null) {
                List<Sede> proveedorSedes = sedeRepository.findAllById(sedeIds);
                proveedor.getSedes().addAll(proveedorSedes);
            }
            proveedorRepository.save(proveedor);
            redirectAttributes.addAttribute("success", "Proveedor creado correctamente.");
            return "redirect:/admin";
        }

        var opt = proveedorRepository.findById(idProv);
        if (opt.isEmpty()) {
            redirectAttributes.addAttribute("error", "Proveedor no encontrado para actualizar.");
            return "redirect:/admin";
        }
        if (proveedorRepository.findByNitIgnoreCase(nit).filter(p -> !p.getIdProv().equals(idProv)).isPresent()) {
            redirectAttributes.addAttribute("error", "Hay otro proveedor con el mismo NIT.");
            return "redirect:/admin";
        }

        Proveedor proveedor = opt.get();
        proveedor.setNit(nit);
        proveedor.setNombre(nombre);
        proveedor.setCiudad(ciudad);
        proveedor.setDireccion(direccion);
        proveedor.setTelefono(telefono);
        proveedor.setCorreo(correo);
        proveedor.getSedes().clear();
        if (sedeIds != null) {
            List<Sede> proveedorSedes = sedeRepository.findAllById(sedeIds);
            proveedor.getSedes().addAll(proveedorSedes);
        }
        proveedorRepository.save(proveedor);
        redirectAttributes.addAttribute("success", "Proveedor actualizado correctamente.");
        return "redirect:/admin";
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
            @RequestParam Rol rol,
            @RequestParam Integer sedeId,
            RedirectAttributes redirectAttributes) {

        // Use final trimmed locals to avoid lambda capture issues
        final String cedulaTrim = cedula == null ? null : cedula.trim();
        final String nombreTrim = nombre == null ? null : nombre.trim();
        final String apellidoTrim = apellido == null ? null : apellido.trim();
        final String nombreUsuarioTrim = nombreUsuario == null ? null : nombreUsuario.trim();
        final String contrasenaTrim = contrasena == null ? null : contrasena.trim();

        // Validación de campos obligatorios (la contraseña puede omitirse en edición)
        if (cedulaTrim == null || cedulaTrim.isBlank()
                || nombreTrim == null || nombreTrim.isBlank()
                || apellidoTrim == null || apellidoTrim.isBlank()
                || nombreUsuarioTrim == null || nombreUsuarioTrim.isBlank()
                || sedeId == null) {
            redirectAttributes.addAttribute("error",
                    "Todos los campos del usuario son obligatorios, excepto la contraseña al editar.");
            return "redirect:/admin";
        }

        if (usuarioRepository.findByCedula(cedulaTrim).filter(u -> !u.getIdUsuario().equals(idUsuario)).isPresent()) {
            redirectAttributes.addAttribute("error", "Ya existe un usuario registrado con esa cédula.");
            return "redirect:/admin";
        }

        Sede sede = sedeRepository.findById(sedeId).orElse(null);
        if (sede == null) {
            redirectAttributes.addAttribute("error", "La sede seleccionada no es válida.");
            return "redirect:/admin";
        }

        if (idUsuario == null) {
            if (usuarioRepository.existsByCedula(cedulaTrim)) {
                redirectAttributes.addAttribute("error", "Ya existe un usuario registrado con esa cédula.");
                return "redirect:/admin";
            }
            if (usuarioRepository.existsByNombreUsuario(nombreUsuarioTrim)) {
                redirectAttributes.addAttribute("error", "El nombre de usuario ya está en uso.");
                return "redirect:/admin";
            }
            if (contrasenaTrim == null || contrasenaTrim.isBlank()) {
                redirectAttributes.addAttribute("error", "La contraseña es obligatoria para crear un usuario.");
                return "redirect:/admin";
            }
            Usuario usuario = new Usuario();
            usuario.setCedula(cedulaTrim);
            usuario.setNombre(nombreTrim);
            usuario.setApellido(apellidoTrim);
            usuario.setCargo(cargo);
            usuario.setNombreUsuario(nombreUsuarioTrim);
            usuario.setContraseña(passwordEncoder.encode(contrasenaTrim));
            usuario.setRol(rol);
            usuario.setSede(sede);
            usuarioRepository.save(usuario);
            redirectAttributes.addAttribute("success", "Usuario creado correctamente.");
            return "redirect:/admin";
        }

        var opt = usuarioRepository.findById(idUsuario);
        if (opt.isEmpty()) {
            redirectAttributes.addAttribute("error", "Usuario no encontrado para actualizar.");
            return "redirect:/admin";
        }
        Usuario usuario = opt.get();
        if (usuarioRepository.existsByCedula(cedulaTrim) && !usuario.getCedula().equalsIgnoreCase(cedulaTrim)) {
            var duplicate = usuarioRepository.findAll().stream()
                    .filter(u -> u.getCedula().equalsIgnoreCase(cedulaTrim) && !u.getIdUsuario().equals(idUsuario))
                    .findAny();
            if (duplicate.isPresent()) {
                redirectAttributes.addAttribute("error", "Ya existe un usuario registrado con esa cédula.");
                return "redirect:/admin";
            }
        }
        if (usuarioRepository.findByNombreUsuario(nombreUsuarioTrim).filter(u -> !u.getIdUsuario().equals(idUsuario))
                .isPresent()) {
            redirectAttributes.addAttribute("error", "El nombre de usuario ya está en uso.");
            return "redirect:/admin";
        }
        usuario.setCedula(cedulaTrim);
        usuario.setNombre(nombreTrim);
        usuario.setApellido(apellidoTrim);
        usuario.setCargo(cargo);
        usuario.setNombreUsuario(nombreUsuarioTrim);
        if (contrasenaTrim != null && !contrasenaTrim.isBlank()) {
            usuario.setContraseña(passwordEncoder.encode(contrasenaTrim));
        }
        usuario.setRol(rol);
        usuario.setSede(sede);
        usuarioRepository.save(usuario);
        redirectAttributes.addAttribute("success", "Usuario actualizado correctamente.");
        return "redirect:/admin";
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
            @RequestParam(required = false) List<BigDecimal> presentacionPrecios) {

        codigoInventario = codigoInventario == null ? null : codigoInventario.trim();
        nombre = nombre == null ? null : nombre.trim();
        if (codigoInventario == null || codigoInventario.isBlank() || nombre == null || nombre.isBlank()) {
            redirectAttributes.addAttribute("error",
                    "El código de inventario y el nombre del producto son obligatorios.");
            return "redirect:/admin";
        }

        try {
            if (idProducto == null) {
                if (productoRepository.findByCodigoInventario(codigoInventario).isPresent()) {
                    redirectAttributes.addAttribute("error", "Ya existe un producto con ese código de inventario.");
                    return "redirect:/admin";
                }
                Producto producto = new Producto();
                producto.setCodigoInventario(codigoInventario);
                producto.setNombre(nombre);
                producto.setCategoria(categoria);
                productoRepository.save(producto);
                aplicarPresentaciones(producto, presentacionNombres, presentacionCantidades, presentacionUnidades,
                        presentacionPrecios);
                productoRepository.save(producto);
                redirectAttributes.addAttribute("success", "Producto creado correctamente.");
                return "redirect:/admin";
            }

            var opt = productoRepository.findById(idProducto);
            if (opt.isEmpty()) {
                redirectAttributes.addAttribute("error", "Producto no encontrado para actualizar.");
                return "redirect:/admin";
            }
            if (productoRepository.findByCodigoInventario(codigoInventario)
                    .filter(p -> !p.getIdProducto().equals(idProducto)).isPresent()) {
                redirectAttributes.addAttribute("error", "Hay otro producto con el mismo código de inventario.");
                return "redirect:/admin";
            }
            Producto producto = opt.get();
            producto.setCodigoInventario(codigoInventario);
            producto.setNombre(nombre);
            producto.setCategoria(categoria);
            producto.getPresentaciones().clear();
            aplicarPresentaciones(producto, presentacionNombres, presentacionCantidades, presentacionUnidades,
                    presentacionPrecios);
            productoRepository.save(producto);
            redirectAttributes.addAttribute("success", "Producto actualizado correctamente.");
            return "redirect:/admin";
} catch (DataAccessException e) {
            // Incluir la causa real para poder diagnosticar el problema (p. ej.
            // tabla 'presentacion_producto' inexistente en la base de datos).
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

private void aplicarPresentaciones(Producto producto,
            List<String> nombres,
            List<Integer> cantidades,
            List<String> unidades,
            List<BigDecimal> precios) {

        // Determinar la mayor cantidad de filas entre todas las listas para
        // no perder filas que solo tengan cantidad, unidad o precio (sin nombre).
        int maxFilas = 0;
        if (nombres != null) maxFilas = Math.max(maxFilas, nombres.size());
        if (cantidades != null) maxFilas = Math.max(maxFilas, cantidades.size());
        if (unidades != null) maxFilas = Math.max(maxFilas, unidades.size());
        if (precios != null) maxFilas = Math.max(maxFilas, precios.size());

        boolean hayAlgunaFilaConDatos = false;

        for (int i = 0; i < maxFilas; i++) {
            String nombrePres = nombres != null && i < nombres.size() && nombres.get(i) != null
                    ? nombres.get(i).trim() : null;
            String unidadPres = unidades != null && i < unidades.size() && unidades.get(i) != null
                    ? unidades.get(i).trim() : null;
            Integer cantidadPres = cantidades != null && i < cantidades.size() && cantidades.get(i) != null
                    ? cantidades.get(i) : null;
            BigDecimal precioPres = precios != null && i < precios.size() && precios.get(i) != null
                    ? precios.get(i) : null;

            // Guardar la fila si CUALQUIERA de los 4 campos está diligenciado
            boolean tieneDatos = (nombrePres != null && !nombrePres.isBlank())
                    || (unidadPres != null && !unidadPres.isBlank())
                    || cantidadPres != null
                    || precioPres != null;
            if (!tieneDatos) {
                continue;
            }
            hayAlgunaFilaConDatos = true;

            // El nombre de la presentación se toma, en orden de prioridad:
            // 1) el campo "presentación" (si está diligenciado),
            // 2) el campo "unidad" (cuando solo se llenó la unidad, ej. "Kg"),
            // 3) "Unidad" por defecto.
            String nombreFinal;
            if (nombrePres != null && !nombrePres.isBlank()) {
                nombreFinal = nombrePres;;
            } else {
                nombreFinal = "";
            }

            PresentacionProducto pres = new PresentacionProducto();
            pres.setPresentacion(nombreFinal);
            // Si la cantidad es 1 o está vacía, se guarda null para evitar la
            // redundancia de registrar "1 unidad".
        
            pres.setCantidad(cantidadPres != null && cantidadPres != 1 ? cantidadPres : null);
            pres.setUnidad(unidadPres != null && !unidadPres.isBlank() ? unidadPres : "Und");
            pres.setPrecio(precioPres != null ? precioPres : BigDecimal.ZERO);
            producto.addPresentacion(pres);
        }

        // Si el usuario NO llenó NINGÚN campo de presentaciones, se crea por
        // defecto una presentación "Unidad" (en vez de "Sin presentación").
        if (!hayAlgunaFilaConDatos) {
            PresentacionProducto pres = new PresentacionProducto();
            pres.setPresentacion("");
            pres.setCantidad(null);
            pres.setUnidad("Und");
            pres.setPrecio(BigDecimal.ZERO);
            producto.addPresentacion(pres);
        }
    }

    @PostMapping("/admin/sedes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarSede(@RequestParam(required = false) Integer idSede,
            @RequestParam String nombre,
            @RequestParam String prefijoCiudad,
            @RequestParam(required = false) String direccion,
            RedirectAttributes redirectAttributes) {

        nombre = nombre == null ? null : nombre.trim();
        prefijoCiudad = prefijoCiudad == null ? null : prefijoCiudad.trim();
        if (nombre == null || nombre.isBlank() || prefijoCiudad == null || prefijoCiudad.isBlank()) {
            redirectAttributes.addAttribute("error", "El nombre y el prefijo de la sede son obligatorios.");
            return "redirect:/admin";
        }

        if (idSede == null) {
            if (sedeRepository.existsByNombreIgnoreCase(nombre)) {
                redirectAttributes.addAttribute("error", "Ya existe una sede con ese nombre.");
                return "redirect:/admin";
            }
            if (sedeRepository.existsByPrefijoCiudadIgnoreCase(prefijoCiudad)) {
                redirectAttributes.addAttribute("error", "Ya existe una sede con ese prefijo de ciudad.");
                return "redirect:/admin";
            }
            Sede sede = new Sede();
            sede.setNombre(nombre);
            sede.setPrefijoCiudad(prefijoCiudad);
            sede.setDireccion(direccion);
            sedeRepository.save(sede);
            redirectAttributes.addAttribute("success", "Sede creada correctamente.");
            return "redirect:/admin";
        }

        var opt = sedeRepository.findById(idSede);
        if (opt.isEmpty()) {
            redirectAttributes.addAttribute("error", "Sede no encontrada para actualizar.");
            return "redirect:/admin";
        }
        Sede existing = opt.get();
        boolean duplicateName = sedeRepository.existsByNombreIgnoreCase(nombre)
                && !existing.getNombre().equalsIgnoreCase(nombre);
        boolean duplicatePrefijo = sedeRepository.existsByPrefijoCiudadIgnoreCase(prefijoCiudad)
                && !existing.getPrefijoCiudad().equalsIgnoreCase(prefijoCiudad);
        if (duplicateName) {
            redirectAttributes.addAttribute("error", "Ya existe una sede con ese nombre.");
            return "redirect:/admin";
        }
        if (duplicatePrefijo) {
            redirectAttributes.addAttribute("error", "Ya existe una sede con ese prefijo de ciudad.");
            return "redirect:/admin";
        }
        existing.setNombre(nombre);
        existing.setPrefijoCiudad(prefijoCiudad);
        existing.setDireccion(direccion);
        sedeRepository.save(existing);
        redirectAttributes.addAttribute("success", "Sede actualizada correctamente.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/centros-costo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarCentroCosto(@RequestParam(required = false) Integer idCentroCosto,
            @RequestParam String nombre,
            @RequestParam Integer sedeId,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String direccion,
            RedirectAttributes redirectAttributes) {

        nombre = nombre == null ? null : nombre.trim();
        codigo = codigo == null ? null : codigo.trim();
        direccion = direccion == null ? "" : direccion.trim();
        if (nombre == null || nombre.isBlank() || sedeId == null) {
            redirectAttributes.addAttribute("error", "El nombre del centro de costo y la sede son obligatorios.");
            return "redirect:/admin";
        }
        Sede sede = sedeRepository.findById(sedeId).orElse(null);
        if (sede == null) {
            redirectAttributes.addAttribute("error", "La sede seleccionada no es válida.");
            return "redirect:/admin";
        }

        if (idCentroCosto == null) {
            if (centroCostoRepository.existsByNombreIgnoreCaseAndSedeIdSede(nombre, sedeId)) {
                redirectAttributes.addAttribute("error",
                        "Ya existe un centro de costo con ese nombre en la sede seleccionada.");
                return "redirect:/admin";
            }
            if (codigo != null && !codigo.isBlank()
                    && centroCostoRepository.existsByCodigoIgnoreCase(codigo)) {
                redirectAttributes.addAttribute("error",
                        "Ya existe un centro de costo con el código '" + codigo + "'.");
                return "redirect:/admin";
            }
            CentroCosto centroCosto = new CentroCosto();
            centroCosto.setNombre(nombre);
            centroCosto.setSede(sede);
            centroCosto.setCodigo(codigo);
            centroCosto.setDireccion(direccion);
            centroCostoRepository.save(centroCosto);
            redirectAttributes.addAttribute("success", "Centro de costo creado correctamente.");
            return "redirect:/admin";
        }

        var opt = centroCostoRepository.findById(idCentroCosto);
        if (opt.isEmpty()) {
            redirectAttributes.addAttribute("error", "Centro de costo no encontrado para actualizar.");
            return "redirect:/admin";
        }
        CentroCosto existing = opt.get();
        if (centroCostoRepository.existsByNombreIgnoreCaseAndSedeIdSede(nombre, sedeId)
                && !(existing.getNombre().equalsIgnoreCase(nombre) && existing.getSede().getIdSede().equals(sedeId))) {
            redirectAttributes.addAttribute("error",
                    "Ya existe otro centro de costo con ese nombre en la sede seleccionada.");
            return "redirect:/admin";
        }
        if (codigo != null && !codigo.isBlank()
                && centroCostoRepository.existsByCodigoIgnoreCase(codigo)
                && !(existing.getCodigo() != null && existing.getCodigo().equalsIgnoreCase(codigo))) {
            redirectAttributes.addAttribute("error",
                    "Ya existe otro centro de costo con el código '" + codigo + "'.");
            return "redirect:/admin";
        }
        existing.setNombre(nombre);
        existing.setSede(sede);
        existing.setCodigo(codigo);
        existing.setDireccion(direccion);
        centroCostoRepository.save(existing);
        redirectAttributes.addAttribute("success", "Centro de costo actualizado correctamente.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/productos/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteProducto(@PathVariable Integer id) {
        if (!productoRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Producto no encontrado"));
        }
        presentacionProductoRepository.deleteByProductoIdProducto(id);
        productoRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", "Producto eliminado"));
    }

    // Endpoint para cargar las presentaciones de un producto al editar en admin
    @GetMapping("/admin/producto/{id}/presentaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseBody
    public List<PresentacionProducto> obtenerPresentacionesProducto(@PathVariable Integer id) {
        return presentacionProductoRepository.findByProductoIdProducto(id);
    }

    @PostMapping("/admin/centros-costo/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteCentro(@PathVariable Integer id) {
        if (!centroCostoRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Centro no encontrado"));
        }
        centroCostoRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", "Centro eliminado"));
    }

    @PostMapping("/admin/usuarios/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteUsuario(@PathVariable Integer id) {
        if (!usuarioRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", "Usuario eliminado"));
    }

    @PostMapping("/admin/proveedores/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteProveedor(@PathVariable Integer id) {
        if (!proveedorRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Proveedor no encontrado"));
        }
        proveedorRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", "Proveedor eliminado"));
    }

    @PostMapping("/admin/sedes/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteSede(@PathVariable Integer id) {
        if (!sedeRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sede no encontrada"));
        }
        sedeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", "Sede eliminada"));
    }
}

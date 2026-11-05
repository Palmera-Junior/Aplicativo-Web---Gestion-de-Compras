package com.palmera_junior.gestion_compras.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoHistorialEstado;
import com.palmera_junior.gestion_compras.rrhh.service.IEmpleadoService;

/** Vistas iniciales del mÃ³dulo de Recursos Humanos. */
@Controller
public class RecursosHumanosController {

    private final IEmpleadoService empleadoService;

    public RecursosHumanosController(IEmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @GetMapping("/recursos-humanos/dashboard")
    public String dashboard() {
        return "recursos-humanos-dashboard";
    }

    @GetMapping("/recursos-humanos/principal")
    public String principal(
            @RequestParam(required = false) Integer empleadoId,
            Model model) {
        List<Empleado> empleados = empleadoService.listar();
        model.addAttribute("empleados", empleados);
        model.addAttribute("totalEmpleados", empleados.size());
        model.addAttribute("cantidadRegistroInicial",
                contarPorEstado(empleados, EstadoEmpleado.REGISTRO_INICIAL));
        model.addAttribute("cantidadProcesoContratacion",
                contarPorEstado(empleados, EstadoEmpleado.PROCESO_CONTRATACION));
        model.addAttribute("cantidadActivos", contarPorEstado(empleados, EstadoEmpleado.ACTIVO));
        model.addAttribute("cantidadRetirados", contarPorEstado(empleados, EstadoEmpleado.RETIRADO));
        model.addAttribute("empleadoSeleccionado", null);
        model.addAttribute("historialEstados", List.of());

        if (empleadoId != null) {
            Empleado empleado = empleadoService.buscarPorId(empleadoId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "No existe el empleado solicitado"));
            List<EmpleadoHistorialEstado> historial =
                    empleadoService.listarHistorialEstado(empleadoId);
            model.addAttribute("empleadoSeleccionado", empleado);
            model.addAttribute("historialEstados", historial);
        }
        return "recursos-humanos-principal";
    }

    private long contarPorEstado(List<Empleado> empleados, EstadoEmpleado estado) {
        return empleados.stream()
                .filter(empleado -> empleado.getEstado() == estado)
                .count();
    }
}

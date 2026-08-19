package com.palmera_junior.gestion_compras.service.dashboard;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

public interface IDashboardService {
    String prepararModeloDashboard(int page, int size, String q, String fechaDesde, String fechaHasta,
            String estado, Model model, Authentication authentication);
}

        (() => {
            const filtrosRrhh = document.querySelector(".rrhh-filters");
            filtrosRrhh?.querySelectorAll("select").forEach((select) => {
                select.addEventListener("change", () => filtrosRrhh.requestSubmit());
            });
            document.querySelectorAll("dialog").forEach((dialogo) => {
                dialogo.addEventListener("cancel", (evento) => evento.preventDefault());
            });

            const hoyLocal = () => {
                const ahora = new Date();
                return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, "0")}-${String(ahora.getDate()).padStart(2, "0")}`;
            };
            const mostrarErrorCampo = (campo, mensaje) => {
                const contenedor = campo.closest(".rrhh-form-field") || campo.parentElement;
                let ayuda = contenedor.querySelector(`[data-validacion-para="${campo.id}"]`);
                if (!ayuda) {
                    ayuda = document.createElement("small");
                    ayuda.className = "rrhh-field-feedback";
                    ayuda.dataset.validacionPara = campo.id;
                    ayuda.setAttribute("aria-live", "polite");
                    contenedor.append(ayuda);
                }
                ayuda.textContent = mensaje;
                ayuda.hidden = !mensaje;
                campo.setCustomValidity(mensaje);
                campo.setAttribute("aria-invalid", mensaje ? "true" : "false");
            };
            document.querySelectorAll("[data-solo-digitos]").forEach((campo) => {
                const validar = () => {
                    const invalido = campo.value !== "" && !/^[0-9]+$/.test(campo.value);
                    mostrarErrorCampo(campo, invalido ? "Este campo solo puede contener números." : "");
                };
                campo.addEventListener("beforeinput", (evento) => {
                    if (evento.data && !/^[0-9]+$/.test(evento.data)) {
                        evento.preventDefault();
                        mostrarErrorCampo(campo, "Este campo solo puede contener números.");
                        campo.reportValidity();
                    }
                });
                campo.addEventListener("paste", (evento) => {
                    const texto = evento.clipboardData?.getData("text") || "";
                    if (!/^[0-9]+$/.test(texto)) {
                        evento.preventDefault();
                        mostrarErrorCampo(campo, "Este campo solo puede contener números.");
                        campo.reportValidity();
                    }
                });
                campo.addEventListener("input", validar);
                campo.addEventListener("blur", () => {
                    validar();
                    if (campo.validationMessage) campo.reportValidity();
                });
                validar();
            });
            document.querySelectorAll("[data-correo-valido]").forEach((campo) => {
                const validar = () => {
                    const correo = campo.value.trim();
                    const valido = !correo || /^[^\s@]+@[^\s@]+\.[A-Za-z]{2,}$/.test(correo);
                    mostrarErrorCampo(campo, valido ? "" : "Correo no válido. Usa el formato ejemplo@ejemplo.com.");
                };
                campo.addEventListener("input", validar);
                campo.addEventListener("blur", validar);
                validar();
            });
            document.querySelectorAll("[data-no-negativo]").forEach((campo) => {
                const mostrarAyuda = (mensaje) => {
                    let ayuda = campo.closest(".rrhh-form-field")
                        ?.querySelector(`[data-validacion-para="${campo.id}"]`);
                    if (!ayuda) {
                        ayuda = document.createElement("small");
                        ayuda.className = "rrhh-field-feedback";
                        ayuda.dataset.validacionPara = campo.id;
                        ayuda.setAttribute("aria-live", "polite");
                        (campo.closest(".rrhh-form-field") || campo.parentElement).append(ayuda);
                    }
                    ayuda.textContent = mensaje;
                    ayuda.hidden = !mensaje;
                    campo.setAttribute("aria-invalid", mensaje ? "true" : "false");
                };
                const validar = () => {
                    const valor = campo.valueAsNumber;
                    const maximo = Number(campo.dataset.maximo);
                    const negativo = campo.value !== "" && Number.isFinite(valor) && valor < 0;
                    const superaMaximo = campo.value !== "" && Number.isFinite(maximo)
                        && Number.isFinite(valor) && valor > maximo;
                    const formatoInvalido = campo.dataset.numericoDecimal !== undefined
                        && (campo.validity.badInput || campo.validity.stepMismatch);
                    const mensaje = negativo
                        ? "El valor no puede ser negativo."
                        : (superaMaximo ? `El valor máximo permitido es ${maximo}.`
                            : (formatoInvalido
                                ? "Ingresa solo números y hasta dos decimales."
                                : ""));
                    campo.setCustomValidity(mensaje);
                    mostrarAyuda(mensaje);
                };
                if (campo.dataset.numericoDecimal !== undefined) {
                    campo.addEventListener("beforeinput", (evento) => {
                        if (evento.data && !/^[0-9.]$/.test(evento.data)) {
                            evento.preventDefault();
                            campo.setCustomValidity("Ingresa solo números y hasta dos decimales.");
                            mostrarAyuda(campo.validationMessage);
                            campo.reportValidity();
                        } else if (evento.data === "." && campo.value.includes(".")) {
                            evento.preventDefault();
                            campo.setCustomValidity("Ingresa solo números y hasta dos decimales.");
                            mostrarAyuda(campo.validationMessage);
                            campo.reportValidity();
                        }
                    });
                    campo.addEventListener("paste", (evento) => {
                        const texto = evento.clipboardData?.getData("text") || "";
                        if (!/^[0-9]+(?:\.[0-9]{0,2})?$/.test(texto)) {
                            evento.preventDefault();
                            campo.setCustomValidity("Ingresa solo números y hasta dos decimales.");
                            mostrarAyuda(campo.validationMessage);
                            campo.reportValidity();
                        }
                    });
                }
                campo.addEventListener("input", validar);
                campo.addEventListener("change", validar);
                campo.addEventListener("blur", () => {
                    validar();
                    if (campo.validationMessage) campo.reportValidity();
                });
                validar();
            });
            const fechaSiguiente = (fecha) => {
                if (!fecha) return "";
                const valor = new Date(`${fecha}T00:00:00Z`);
                valor.setUTCDate(valor.getUTCDate() + 1);
                return valor.toISOString().slice(0, 10);
            };
            const agregarValidacionFechas = (inicio, fin) => {
                if (!inicio || !fin) return;
                const validar = () => {
                    fin.min = fechaSiguiente(inicio.value);
                    const invalida = Boolean(inicio.value && fin.value && fin.value <= inicio.value);
                    mostrarErrorCampo(fin, invalida
                        ? "La fecha de finalización debe ser posterior a la fecha de inicio."
                        : "");
                };
                [inicio, fin].forEach((campo) => {
                    campo.addEventListener("input", validar);
                    campo.addEventListener("change", validar);
                });
                validar();
            };
            agregarValidacionFechas(
                document.getElementById("fecha-ingreso"),
                document.getElementById("fecha-fin"));
            document.querySelectorAll("[data-fecha-expedicion]").forEach((fechaExpedicion) => {
                const formulario = fechaExpedicion.closest("form");
                const fechaNacimiento = formulario?.querySelector("[data-fecha-nacimiento]");
                const actualizarFechasPersonales = () => {
                    const hoy = hoyLocal();
                    fechaExpedicion.max = hoy;
                    const referencia = fechaExpedicion.value || hoy;
                    if (fechaNacimiento) {
                        const [anio, mes, dia] = referencia.split("-").map(Number);
                        const anioLimite = anio - 18;
                        const diaLimite = Math.min(dia, new Date(anioLimite, mes, 0).getDate());
                        const limiteEdad = `${anioLimite}-${String(mes).padStart(2, "0")}-${String(diaLimite).padStart(2, "0")}`;
                        fechaNacimiento.max = limiteEdad;
                    }
                    mostrarErrorCampo(fechaExpedicion, fechaExpedicion.value > hoy
                        ? "La fecha de expedición no puede ser futura." : "");
                    if (fechaNacimiento) {
                        const fechaLimite = fechaNacimiento.max;
                        mostrarErrorCampo(fechaNacimiento, fechaNacimiento.value > hoy
                            ? "La fecha de nacimiento no puede ser futura."
                            : (fechaNacimiento.value && fechaNacimiento.value > fechaLimite
                                ? "La persona debe tener al menos 18 años en la fecha de expedición."
                                : ""));
                    }
                };
                [fechaExpedicion, fechaNacimiento].filter(Boolean).forEach((campo) => {
                    campo.addEventListener("input", actualizarFechasPersonales);
                    campo.addEventListener("change", actualizarFechasPersonales);
                });
                actualizarFechasPersonales();
            });
            document.querySelectorAll("[data-fecha-ingreso]").forEach((campo) => {
                const validar = () => {
                    const hoy = hoyLocal();
                    campo.min = hoy;
                    mostrarErrorCampo(campo, campo.value && campo.value < hoy
                        ? "La fecha de ingreso no puede ser anterior a hoy." : "");
                };
                campo.addEventListener("input", validar);
                campo.addEventListener("change", validar);
                validar();
            });
            const dialogoQr = document.getElementById("rrhh-qr-dialog");
            const abrirQrSolicitado = () => {
                if (window.location.hash === "#modal-qr" && dialogoQr && !dialogoQr.open) {
                    dialogoQr.showModal();
                }
            };
            window.addEventListener("hashchange", abrirQrSolicitado);
            dialogoQr?.addEventListener("close", () => {
                if (window.location.hash === "#modal-qr") {
                    window.history.replaceState(null, "", window.location.pathname + window.location.search);
                }
            });
            abrirQrSolicitado();
            const confirmacionQr = document.getElementById("rrhh-qr-confirm-dialog");
            document.querySelectorAll("[data-close-qr]").forEach((boton) => {
                boton.addEventListener("click", () => dialogoQr?.close());
            });
            document.querySelectorAll("[data-open-qr-confirm]").forEach((boton) => {
                boton.addEventListener("click", () => confirmacionQr?.showModal());
            });
            document.querySelectorAll("[data-close-qr-confirm]").forEach((boton) => {
                boton.addEventListener("click", () => confirmacionQr?.close());
            });
            if (window.location.hash === "#ficha-asignaciones"
                    || new URLSearchParams(window.location.search).get("pestana") === "asignaciones") {
                const panel = document.getElementById("rrhh-detail-assets");
                if (panel) panel.checked = true;
                if (window.location.hash === "#ficha-asignaciones") {
                    window.history.replaceState(null, "", window.location.pathname
                        + window.location.search + "#ficha-empleado");
                }
                document.getElementById("ficha-empleado").scrollIntoView();
            } else if (window.location.hash === "#ficha-auditoria"
                    || new URLSearchParams(window.location.search).get("pestana") === "auditoria") {
                const panel = document.getElementById("rrhh-detail-audit");
                if (panel) panel.checked = true;
                if (window.location.hash === "#ficha-auditoria") {
                    window.history.replaceState(null, "", window.location.pathname
                        + window.location.search + "#ficha-empleado");
                }
                document.getElementById("ficha-empleado").scrollIntoView();
            } else if (new URLSearchParams(window.location.search).get("pestana") === "documentos") {
                const panel = document.getElementById("rrhh-detail-documents");
                if (panel) panel.checked = true;
                document.getElementById("ficha-empleado").scrollIntoView();
            }
            document.querySelectorAll('input[type="file"][name="acta"]').forEach((input) => {
                input.addEventListener("change", () => {
                    const nombre = input.closest("form")?.querySelector("[data-acta-name]");
                    if (nombre) nombre.textContent = input.files?.[0]?.name || "Sin archivo seleccionado";
                });
            });
            document.querySelectorAll("[data-photo-input]").forEach((input) => {
                input.addEventListener("change", () => {
                    const nombre = input.parentElement.querySelector("[data-photo-name]");
                    if (nombre) {
                        nombre.textContent = input.files?.[0]?.name
                            || input.dataset.existingName
                            || "Sin fotografía cargada";
                    }
                });
            });

            const dialogoMotivo = document.getElementById("rrhh-contract-reason-dialog");
            const formularioMotivo = document.getElementById("rrhh-contract-reason-form");
            const campoMotivo = document.getElementById("rrhh-contract-reason");
            const etiquetaMotivo = document.getElementById("rrhh-contract-reason-label");
            const tituloMotivo = document.getElementById("rrhh-contract-reason-title");
            const botonConfirmarMotivo = document.getElementById("rrhh-contract-reason-submit");
            document.querySelectorAll(".rrhh-contract-action-button").forEach((boton) => {
                boton.addEventListener("click", () => {
                    if (!dialogoMotivo || !formularioMotivo || !campoMotivo) return;
                    formularioMotivo.action = boton.dataset.actionUrl;
                    tituloMotivo.textContent = boton.dataset.actionTitle;
                    etiquetaMotivo.textContent = boton.dataset.reasonLabel;
                    botonConfirmarMotivo.textContent = boton.dataset.submitLabel;
                    formularioMotivo.reset();
                    campoMotivo.setCustomValidity("");
                    dialogoMotivo.showModal();
                    campoMotivo.focus();
                });
            });
            document.querySelectorAll("[data-cerrar-motivo]").forEach((boton) => {
                boton.addEventListener("click", () => dialogoMotivo?.close());
            });
            campoMotivo?.addEventListener("input", () => {
                campoMotivo.setCustomValidity(campoMotivo.value.trim() ? "" : "El motivo es obligatorio.");
            });
            formularioMotivo?.addEventListener("submit", (evento) => {
                if (!campoMotivo.value.trim()) {
                    campoMotivo.setCustomValidity("El motivo es obligatorio.");
                    campoMotivo.reportValidity();
                    evento.preventDefault();
                } else {
                    campoMotivo.value = campoMotivo.value.trim();
                }
            });

            const dialogoVistaPdf = document.getElementById("rrhh-pdf-preview-dialog");
            const marcoVistaPdf = document.getElementById("rrhh-pdf-preview-frame");
            const tituloVistaPdf = document.getElementById("rrhh-pdf-preview-title");
            document.querySelectorAll("[data-pdf-preview-url]").forEach((enlace) => {
                enlace.addEventListener("click", (evento) => {
                    if (!dialogoVistaPdf || !marcoVistaPdf) return;
                    evento.preventDefault();
                    marcoVistaPdf.src = enlace.dataset.pdfPreviewUrl;
                    tituloVistaPdf.textContent = enlace.dataset.pdfPreviewTitle || "Vista previa PDF";
                    dialogoVistaPdf.showModal();
                });
            });
            dialogoVistaPdf?.addEventListener("close", () => {
                if (marcoVistaPdf) marcoVistaPdf.src = "about:blank";
            });

            document.querySelectorAll(".rrhh-document-replace-input").forEach((input) => {
                input.addEventListener("change", () => {
                    if (input.files?.length) input.form?.requestSubmit();
                });
            });
            document.querySelectorAll("form[data-confirm-message]").forEach((formulario) => {
                formulario.addEventListener("submit", (evento) => {
                    if (!window.confirm(formulario.dataset.confirmMessage)) evento.preventDefault();
                });
            });

            const formularioRenovacion = document.getElementById("rrhh-renew-contract-form");
            if (formularioRenovacion) {
                const inicio = document.getElementById("renovacion-inicio");
                const fin = document.getElementById("renovacion-fin");
                const avisoFechas = document.getElementById("rrhh-renew-date-note");
                const fechaCubiertaHasta = formularioRenovacion.dataset.cubiertoHasta;
                let inicioMinimo = "";
                if (fechaCubiertaHasta) {
                    const diaSiguiente = new Date(`${fechaCubiertaHasta}T00:00:00Z`);
                    diaSiguiente.setUTCDate(diaSiguiente.getUTCDate() + 1);
                    inicioMinimo = diaSiguiente.toISOString().slice(0, 10);
                    inicio.min = inicioMinimo;
                }
                const validarFechas = () => {
                    inicio.setCustomValidity("");
                    fin.setCustomValidity("");
                    if (inicioMinimo && inicio.value && inicio.value < inicioMinimo) {
                        inicio.setCustomValidity(`La renovación debe iniciar desde ${inicioMinimo}.`);
                    }
                    const fechaMinimaFin = inicio.value || inicioMinimo;
                    if (fechaMinimaFin) {
                        const diaPosterior = new Date(`${fechaMinimaFin}T00:00:00Z`);
                        diaPosterior.setUTCDate(diaPosterior.getUTCDate() + 1);
                        fin.min = diaPosterior.toISOString().slice(0, 10);
                    } else {
                        fin.removeAttribute("min");
                    }
                    const fechaFinInvalida = fin.value && inicio.value && fin.value <= inicio.value;
                    if (fechaFinInvalida) {
                        fin.setCustomValidity("La fecha de vencimiento debe ser posterior al inicio.");
                    }
                    let ayudaFin = fin.closest(".rrhh-form-field")
                        ?.querySelector(`[data-validacion-para="${fin.id}"]`);
                    if (!ayudaFin) {
                        ayudaFin = document.createElement("small");
                        ayudaFin.className = "rrhh-field-feedback";
                        ayudaFin.dataset.validacionPara = fin.id;
                        ayudaFin.setAttribute("aria-live", "polite");
                        fin.closest(".rrhh-form-field")?.append(ayudaFin);
                    }
                    ayudaFin.textContent = fechaFinInvalida ? fin.validationMessage : "";
                    ayudaFin.hidden = !fechaFinInvalida;
                    fin.setAttribute("aria-invalid", fechaFinInvalida ? "true" : "false");
                    const ayudaInicio = inicio.closest(".rrhh-form-field")
                        ?.querySelector(`[data-validacion-para="${inicio.id}"]`);
                    if (ayudaInicio) {
                        ayudaInicio.textContent = inicio.validationMessage;
                        ayudaInicio.hidden = !inicio.validationMessage;
                    } else if (inicio.validationMessage) {
                        const nuevaAyuda = document.createElement("small");
                        nuevaAyuda.className = "rrhh-field-feedback";
                        nuevaAyuda.dataset.validacionPara = inicio.id;
                        nuevaAyuda.setAttribute("aria-live", "polite");
                        nuevaAyuda.textContent = inicio.validationMessage;
                        inicio.closest(".rrhh-form-field")?.append(nuevaAyuda);
                    }
                    inicio.setAttribute("aria-invalid", inicio.validationMessage ? "true" : "false");
                    if (avisoFechas) {
                        avisoFechas.textContent = inicioMinimo
                            ? `El nuevo contrato podrá iniciar desde ${inicioMinimo}; el periodo actual está cubierto hasta ${fechaCubiertaHasta}.`
                            : "";
                    }
                };
                inicio.addEventListener("input", validarFechas);
                inicio.addEventListener("change", validarFechas);
                fin.addEventListener("input", validarFechas);
                fin.addEventListener("change", validarFechas);
                formularioRenovacion.addEventListener("submit", (evento) => {
                    validarFechas();
                    if (!formularioRenovacion.reportValidity()) evento.preventDefault();
                });
                validarFechas();
            }

            const inputContratoFirmado = document.getElementById("renovacion-contrato-firmado");
            inputContratoFirmado?.addEventListener("change", () => {
                const nombre = document.getElementById("renovacion-contrato-firmado-nombre");
                if (nombre) nombre.textContent = inputContratoFirmado.files?.[0]?.name
                    || "Ningún archivo seleccionado";
            });
        })();
        (() => {
            const form = document.getElementById("rrhh-registration-form");
            if (!form) return;

            const error = document.getElementById("rrhh-upload-error");
            const mensajeVacio = document.getElementById("rrhh-no-adjuntos");
            const botonCompletar = form.dataset.edicionContratacion === "true"
                ? form.querySelector("[data-completar-registro]") : null;
            const avisoCompletar = document.getElementById("rrhh-completion-hint");
            const selectors = form.querySelectorAll("[data-adjunto-requerido]");
            const archivos = form.querySelectorAll('input[type="file"][name^="archivo-"]');
            const camposContratacion = form.querySelectorAll(
                ".step-contract input, .step-contract select, .step-contract textarea"
            );
            const camposCondicionales = form.querySelectorAll("[data-valor-para]");
            const botonVerFaltantes = form.querySelector("[data-ver-faltantes]");
            const tiposAdjuntosVigentes = new Set(
                (form.dataset.adjuntosVigentes || "").split(",").filter(Boolean)
            );
            const maximoArchivo = 10 * 1024 * 1024;
            const maximoSolicitud = 50 * 1024 * 1024;

            const actualizarAdjuntos = () => {
                const seleccionados = new Set(
                    Array.from(selectors).filter((control) => control.checked).map((control) => control.value)
                );
                form.querySelectorAll("[data-tipo-adjunto]").forEach((concepto) => {
                    const visible = seleccionados.has(concepto.dataset.tipoAdjunto);
                    concepto.hidden = !visible;
                    const input = concepto.querySelector('input[type="file"]');
                    if (!visible) input.value = "";
                    actualizarNombreArchivo(concepto, input);
                });
                mensajeVacio.hidden = seleccionados.size > 0;
            };

            const actualizarCamposCondicionales = () => {
                form.querySelectorAll("[data-valor-para]").forEach((campo) => {
                    const checkbox = form.querySelector(
                        `[data-requiere-valor="${campo.dataset.valorPara}"]`
                    );
                    const habilitado = checkbox?.checked === true;
                    campo.disabled = !habilitado;
                    campo.required = habilitado;
                    if (!habilitado) {
                        campo.value = "";
                        campo.setCustomValidity("");
                        const ayuda = campo.closest(".rrhh-form-field")
                            ?.querySelector(`[data-validacion-para="${campo.id}"]`);
                        if (ayuda) ayuda.hidden = true;
                        campo.setAttribute("aria-invalid", "false");
                    } else {
                        campo.dispatchEvent(new Event("input", { bubbles: true }));
                    }
                });
            };

            const obtenerDatosPendientes = () => {
                const faltantes = [];
                const etiquetas = {
                    "tipo-contrato": "Tipo de contrato",
                    "tipo-pago": "Tipo de pago",
                    "fecha-ingreso": "Fecha de ingreso",
                    "fecha-fin": "Fecha de finalización válida",
                    salario: "Salario básico",
                    eps: "EPS",
                    afp: "Fondo de pensión (AFP)",
                    arl: "ARL",
                    "caja-compensacion": "Caja de compensación",
                    "auxilio-transporte": "Valor de auxilio de transporte",
                    "auxilio-rodamiento": "Valor de auxilio de rodamiento",
                    bonificacion: "Valor de bonificación no salarial",
                    "condiciones-especiales": "Descripción de condiciones especiales"
                };
                const fechaIngreso = form.querySelector("#fecha-ingreso");
                const fechaFin = form.querySelector("#fecha-fin");
                const fechaFinInvalida = fechaIngreso.value && fechaFin.value
                    && fechaFin.value <= fechaIngreso.value;
                fechaFin.setCustomValidity(fechaFinInvalida
                    ? "La fecha de finalización debe ser posterior a la fecha de ingreso."
                    : "");

                camposContratacion.forEach((campo) => {
                    if (campo.disabled || campo.type === "hidden" || campo.type === "checkbox") return;
                    if (!campo.checkValidity()) {
                        if (campo.id === "fecha-fin" && fechaFinInvalida) return;
                        faltantes.push(etiquetas[campo.id] || campo.labels?.[0]?.textContent
                            ?.replace("*", "").trim() || "Un campo de contratación");
                    }
                });

                if (fechaFinInvalida) {
                    faltantes.push("La fecha de finalización debe ser posterior a la fecha de ingreso");
                }

                form.querySelectorAll(".step-contract [data-adjunto-requerido]:checked")
                    .forEach((concepto) => {
                        const archivo = form.querySelector(
                            `[name="archivo-${concepto.value}"]`
                        );
                        const adjuntoActual = tiposAdjuntosVigentes.has(concepto.value);
                        const archivosSeleccionados = Array.from(archivo?.files || []);
                        const archivosValidos = archivosSeleccionados.length > 0
                            && archivosSeleccionados.every((item) =>
                                item.name.toLowerCase().endsWith(".pdf"));
                        if (!adjuntoActual && !archivosValidos) {
                            const nombre = concepto.closest("label")?.querySelector("span:last-child")
                                ?.textContent.trim();
                            faltantes.push(`Adjunta ${nombre || "el documento requerido"}`);
                        }
                    });
                return [...new Set(faltantes)];
            };

            const actualizarEstadoCompletar = () => {
                if (!botonCompletar) return;
                actualizarCamposCondicionales();
                const faltantes = obtenerDatosPendientes();
                botonCompletar.disabled = faltantes.length > 0;
                if (avisoCompletar) {
                    avisoCompletar.textContent = faltantes.length > 0
                        ? `Pendiente: ${faltantes.join("; ")}.`
                        : "La información y los adjuntos requeridos están completos.";
                    avisoCompletar.hidden = false;
                }
                return faltantes;
            };

            const actualizarNombreArchivo = (concepto, input) => {
                const estado = concepto.querySelector(".rrhh-upload-selection");
                if (!estado) return;
                const seleccionados = Array.from(input.files || []);
                estado.textContent = seleccionados.length === 0
                    ? "Ningún archivo seleccionado"
                    : seleccionados.length === 1
                        ? seleccionados[0].name
                        : `${seleccionados.length} archivos: ${seleccionados.map((archivo) => archivo.name).join(", ")}`;
            };

            selectors.forEach((control) => control.addEventListener("change", () => {
                actualizarAdjuntos();
                actualizarEstadoCompletar();
            }));
            form.querySelectorAll("[data-tipo-adjunto] input[type='file']").forEach((input) => {
                input.addEventListener("change", () => {
                    actualizarNombreArchivo(input.closest("[data-tipo-adjunto]"), input);
                    error.hidden = true;
                    actualizarEstadoCompletar();
                });
            });
            camposContratacion.forEach((campo) => {
                campo.addEventListener("input", actualizarEstadoCompletar);
                campo.addEventListener("change", actualizarEstadoCompletar);
            });
            camposCondicionales.forEach((campo) => {
                campo.addEventListener("input", actualizarEstadoCompletar);
                campo.addEventListener("change", actualizarEstadoCompletar);
            });
            form.querySelectorAll("[data-requiere-valor]").forEach((checkbox) => {
                checkbox.addEventListener("change", actualizarEstadoCompletar);
            });
            botonVerFaltantes?.addEventListener("click", () => {
                const faltantes = actualizarEstadoCompletar() || [];
                window.mostrarAppToast(
                    faltantes.length
                        ? `Para completar el registro falta: ${faltantes.join("; ")}.`
                        : "No hay datos pendientes. Ya puedes completar el registro.",
                    faltantes.length ? "info" : "success"
                );
            });
            actualizarAdjuntos();
            actualizarEstadoCompletar();

            form.addEventListener("submit", (event) => {
                if (event.submitter === botonCompletar) {
                    const faltantes = actualizarEstadoCompletar() || [];
                    if (faltantes.length) {
                        event.preventDefault();
                        window.mostrarAppToast(
                            `No se puede completar el registro. Falta: ${faltantes.join("; ")}.`,
                            "error"
                        );
                        return;
                    }
                }
                const seleccionados = Array.from(archivos)
                    .flatMap((control) => Array.from(control.files));
                const excedeIndividual = seleccionados.some((archivo) => archivo.size > maximoArchivo);
                const total = seleccionados.reduce((suma, archivo) => suma + archivo.size, 0);
                if (excedeIndividual || total > maximoSolicitud) {
                    event.preventDefault();
                    error.hidden = false;
                    error.textContent = excedeIndividual
                        ? "Cada PDF debe pesar máximo 10 MB."
                        : "El total de archivos adjuntos no puede superar 50 MB. Reduce el tamaño o divide la carga.";
                    window.mostrarAppToast(error.textContent, "error");
                }
            });

            const mensajeExito = document.querySelector(".alert-success");
            const mensajeError = document.querySelector(".alerta-error");
            if (mensajeExito?.textContent.trim()) {
                window.mostrarAppToast(mensajeExito.textContent.trim(), "success");
            }
            if (mensajeError?.textContent.trim()) {
                window.mostrarAppToast(mensajeError.textContent.trim(), "error");
            }
        })();

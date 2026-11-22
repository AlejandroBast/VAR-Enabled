package com.varenabled.aplicacion;

import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;

public interface PublicadorAlertasPort {

    void publicarAlerta(IdPartido idPartido, IdEvento idEvento, String mensaje);
}

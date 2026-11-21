package com.varenabled.aplicacion;

import com.varenabled.dominio.IdPartido;

public interface PublicadorAlertasPort {

    void publicarAlerta(IdPartido idPartido, String mensaje);
}

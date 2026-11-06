package com.iaml.puntoventaapi.sucursal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SucursalJpaRepository extends JpaRepository<SucursalEntity, Long> {

    Optional<SucursalEntity> findByIdAndNegocio_Usuarios_IdAndNegocio_ActivoTrue(Long id, Long usuarioId);

    List<SucursalEntity> findByNegocio_IdAndNegocio_Usuarios_IdAndNegocio_ActivoTrueOrderByIdAsc(Long negocioId, Long usuarioId);

    boolean existsByIdAndNegocio_Id(Long id, Long negocioId);

    @Query("""
        select s.negocio.id from Sucursal s, UsuarioEntity actor
        left join actor.negocio negocioEmpleado
        left join actor.sucursal sucursalEmpleado
        where s.id = :sucursalId and actor.id = :usuarioId and s.negocio.activo = true
          and ((actor.rol.nombre = 'PROPIETARIO' and exists
                (select n.id from Negocio n join n.usuarios propietario
                 where n.id = s.negocio.id and propietario.id = actor.id))
            or (actor.rol.nombre = 'EMPLEADO' and negocioEmpleado.id = s.negocio.id
                and sucursalEmpleado.id = s.id))
        """)
    Optional<Long> negocioAutorizado(@Param("sucursalId") Long sucursalId, @Param("usuarioId") Long usuarioId);

    @Query("""
        select distinct u.id from Sucursal s join s.negocio.usuarios u
        where s.id = :sucursalId and s.negocio.activo = true and u.rol.nombre = 'PROPIETARIO'
        """)
    List<Long> propietariosDeSucursal(@Param("sucursalId") Long sucursalId);

    @Query("select u.id from UsuarioEntity u where u.sucursal.id = :sucursalId and u.rol.nombre = 'EMPLEADO'")
    List<Long> empleadosDeSucursal(@Param("sucursalId") Long sucursalId);
}

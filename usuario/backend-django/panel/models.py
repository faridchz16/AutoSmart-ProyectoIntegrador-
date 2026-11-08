"""
Modelos de AutoSmart para el panel de administracion.

Todas las tablas pertenecen al backend Spring Boot: por eso cada modelo tiene
managed = False. Django las lee (y edita algunos catalogos), pero NUNCA crea,
modifica ni borra su estructura.
"""
from django.db import models


class BitBooleanField(models.BooleanField):
    """Boolean compatible con BIT(1) y TINYINT(1) creados por Hibernate en MariaDB."""

    def from_db_value(self, value, expression, connection):
        if value is None:
            return None
        if isinstance(value, (bytes, bytearray)):
            return int.from_bytes(value, "big") != 0
        return bool(value)


# --------------------------------------------------------------------------
# Usuarios y catalogos
# --------------------------------------------------------------------------

class Rol(models.Model):
    id_rol = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=50, unique=True)

    class Meta:
        managed = False
        db_table = "roles"
        verbose_name = "rol"
        verbose_name_plural = "roles"

    def __str__(self):
        return self.nombre


class Usuario(models.Model):
    id_usuario = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=100)
    correo = models.CharField(max_length=100, unique=True)
    password_hash = models.CharField(max_length=255)
    rol = models.ForeignKey(Rol, models.DO_NOTHING, db_column="id_rol", null=True)
    telefono = models.CharField(max_length=15, null=True, blank=True)
    activo = BitBooleanField(default=True)
    creado_en = models.DateTimeField(null=True, blank=True)
    estado_cambiado_en = models.DateTimeField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = "usuarios"
        verbose_name = "usuario de AutoSmart"
        verbose_name_plural = "usuarios de AutoSmart"
        ordering = ["nombre"]

    def __str__(self):
        return f"{self.nombre} ({self.correo})"


class Especialidad(models.Model):
    id_especialidad = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=60, unique=True)

    class Meta:
        managed = False
        db_table = "especialidades"
        verbose_name = "especialidad"
        verbose_name_plural = "especialidades"

    def __str__(self):
        return self.nombre


class Vehiculo(models.Model):
    id_vehiculo = models.BigAutoField(primary_key=True)
    placa = models.CharField(max_length=15, unique=True)
    marca = models.CharField(max_length=50)
    modelo = models.CharField(max_length=50)
    anio = models.IntegerField("año")
    kilometraje = models.IntegerField(null=True, blank=True)
    cliente = models.ForeignKey(Usuario, models.DO_NOTHING, db_column="id_cliente", null=True,
                                related_name="vehiculos")
    activo = BitBooleanField(default=True)

    class Meta:
        managed = False
        db_table = "vehiculos"
        verbose_name = "vehículo"
        verbose_name_plural = "vehículos"
        ordering = ["placa"]

    def __str__(self):
        return f"{self.placa} - {self.marca} {self.modelo}"


class Servicio(models.Model):
    class Categoria(models.TextChoices):
        PREVENTIVO = "PREVENTIVO", "Preventivo"
        CORRECTIVO = "CORRECTIVO", "Correctivo"

    id_servicio = models.BigAutoField(primary_key=True)
    nombre = models.CharField(max_length=100, unique=True)
    descripcion = models.CharField(max_length=500, null=True, blank=True)
    precio_base = models.DecimalField(max_digits=10, decimal_places=2)
    duracion_minutos = models.IntegerField()
    activo = BitBooleanField(default=True)
    categoria = models.CharField(max_length=15, choices=Categoria.choices, default=Categoria.PREVENTIVO)
    especialidad = models.ForeignKey(Especialidad, models.DO_NOTHING, db_column="id_especialidad",
                                     null=True, blank=True)

    class Meta:
        managed = False
        db_table = "servicios"
        verbose_name = "servicio"
        verbose_name_plural = "servicios"
        ordering = ["nombre"]

    def __str__(self):
        return self.nombre


class HorarioMecanico(models.Model):
    class Dia(models.IntegerChoices):
        LUNES = 1, "Lunes"
        MARTES = 2, "Martes"
        MIERCOLES = 3, "Miércoles"
        JUEVES = 4, "Jueves"
        VIERNES = 5, "Viernes"
        SABADO = 6, "Sábado"
        DOMINGO = 7, "Domingo"

    id_horario = models.AutoField(primary_key=True)
    mecanico = models.ForeignKey(Usuario, models.DO_NOTHING, db_column="id_mecanico",
                                 related_name="horarios")
    dia_semana = models.IntegerField(choices=Dia.choices)
    hora_inicio = models.TimeField()
    hora_fin = models.TimeField()

    class Meta:
        managed = False
        db_table = "horario_mecanico"
        verbose_name = "horario de mecánico"
        verbose_name_plural = "horarios de mecánicos"
        ordering = ["mecanico", "dia_semana"]

    def __str__(self):
        return f"{self.mecanico.nombre} - {self.get_dia_semana_display()}"


class FechaBloqueada(models.Model):
    id_bloqueo = models.AutoField(primary_key=True)
    mecanico = models.ForeignKey(Usuario, models.DO_NOTHING, db_column="id_mecanico",
                                 related_name="fechas_bloqueadas")
    fecha = models.DateField()
    motivo = models.CharField(max_length=200, null=True, blank=True)

    class Meta:
        managed = False
        db_table = "fecha_bloqueada"
        verbose_name = "fecha bloqueada"
        verbose_name_plural = "fechas bloqueadas"
        ordering = ["-fecha"]

    def __str__(self):
        return f"{self.mecanico.nombre} - {self.fecha}"


class Feriado(models.Model):
    id_feriado = models.AutoField(primary_key=True)
    fecha = models.DateField(unique=True)
    descripcion = models.CharField(max_length=100)

    class Meta:
        managed = False
        db_table = "feriados"
        verbose_name = "feriado"
        verbose_name_plural = "feriados"
        ordering = ["fecha"]

    def __str__(self):
        return f"{self.fecha} - {self.descripcion}"


class ConfiguracionTaller(models.Model):
    dia_semana = models.IntegerField(primary_key=True, choices=HorarioMecanico.Dia.choices)
    abierto = BitBooleanField()
    hora_apertura = models.TimeField()
    hora_cierre = models.TimeField()

    class Meta:
        managed = False
        db_table = "configuracion_taller"
        verbose_name = "horario del taller"
        verbose_name_plural = "horario del taller"
        ordering = ["dia_semana"]

    def __str__(self):
        return self.get_dia_semana_display()


# --------------------------------------------------------------------------
# Citas y ordenes de trabajo
# --------------------------------------------------------------------------

class Cita(models.Model):
    class Estado(models.TextChoices):
        PENDIENTE = "PENDIENTE", "Pendiente"
        CONFIRMADA = "CONFIRMADA", "Confirmada"
        RECHAZADA = "RECHAZADA", "Rechazada"
        CANCELADA = "CANCELADA", "Cancelada"
        EN_PROCESO = "EN_PROCESO", "En proceso"
        ESPERA_APROBACION = "ESPERA_APROBACION", "Espera aprobación"
        FINALIZADA = "FINALIZADA", "Finalizada"

    id_cita = models.BigAutoField(primary_key=True)
    ticket = models.CharField(max_length=20, unique=True)
    cliente = models.ForeignKey(Usuario, models.DO_NOTHING, db_column="id_cliente",
                                related_name="citas_como_cliente")
    mecanico = models.ForeignKey(Usuario, models.DO_NOTHING, db_column="id_mecanico",
                                 related_name="citas_como_mecanico")
    vehiculo = models.ForeignKey(Vehiculo, models.DO_NOTHING, db_column="id_vehiculo", related_name="citas")
    servicio = models.ForeignKey(Servicio, models.DO_NOTHING, db_column="id_servicio", related_name="citas")
    fecha = models.DateField()
    hora_inicio = models.TimeField()
    hora_fin = models.TimeField()
    estado = models.CharField(max_length=20, choices=Estado.choices)
    precio = models.DecimalField(max_digits=10, decimal_places=2)
    motivo_rechazo = models.CharField(max_length=300, null=True, blank=True)

    class Meta:
        managed = False
        db_table = "citas"
        verbose_name = "cita"
        verbose_name_plural = "citas"
        ordering = ["-fecha", "-hora_inicio"]

    def __str__(self):
        return self.ticket


class OrdenTrabajo(models.Model):
    id_orden = models.BigAutoField(primary_key=True)
    cita = models.OneToOneField(Cita, models.DO_NOTHING, db_column="id_cita", related_name="orden")
    fecha_inicio = models.DateTimeField()
    fecha_fin = models.DateTimeField(null=True, blank=True)
    diagnostico = models.CharField(max_length=2000, null=True, blank=True)
    fecha_diagnostico = models.DateTimeField(null=True, blank=True)
    costo_repuestos = models.DecimalField(max_digits=10, decimal_places=2)

    class Meta:
        managed = False
        db_table = "ordenes_trabajo"
        verbose_name = "orden de trabajo"
        verbose_name_plural = "órdenes de trabajo"
        ordering = ["-fecha_inicio"]

    def __str__(self):
        return f"Orden {self.id_orden} ({self.cita.ticket})"


class Checklist(models.Model):
    id_checklist = models.BigAutoField(primary_key=True)
    orden = models.OneToOneField(OrdenTrabajo, models.DO_NOTHING, db_column="id_orden", related_name="checklist")
    fecha_registro = models.DateTimeField()
    observacion_general = models.CharField(max_length=300, null=True, blank=True)

    class Meta:
        managed = False
        db_table = "checklists"
        verbose_name = "checklist de recepción"
        verbose_name_plural = "checklists de recepción"

    def __str__(self):
        return f"Checklist de la orden {self.orden_id}"


class ChecklistItem(models.Model):
    id_item = models.BigAutoField(primary_key=True)
    checklist = models.ForeignKey(Checklist, models.DO_NOTHING, db_column="id_checklist", related_name="items")
    elemento = models.CharField(max_length=20)
    estado = models.CharField(max_length=10)
    observacion = models.CharField(max_length=300, null=True, blank=True)

    class Meta:
        managed = False
        db_table = "checklist_items"
        verbose_name = "ítem del checklist"
        verbose_name_plural = "ítems del checklist"

    def __str__(self):
        return f"{self.elemento}: {self.estado}"


class Deficiencia(models.Model):
    id_deficiencia = models.BigAutoField(primary_key=True)
    orden = models.ForeignKey(OrdenTrabajo, models.DO_NOTHING, db_column="id_orden", related_name="deficiencias")
    categoria = models.CharField(max_length=20)
    severidad = models.CharField(max_length=10)
    descripcion = models.CharField(max_length=500)
    fecha_registro = models.DateTimeField()

    class Meta:
        managed = False
        db_table = "deficiencias"
        verbose_name = "deficiencia"
        verbose_name_plural = "deficiencias"

    def __str__(self):
        return f"{self.categoria} ({self.severidad})"


class FotoDeficiencia(models.Model):
    id_foto = models.BigAutoField(primary_key=True)
    deficiencia = models.ForeignKey(Deficiencia, models.DO_NOTHING, db_column="id_deficiencia",
                                    related_name="fotos")
    nombre_archivo = models.CharField(max_length=100, unique=True)
    tipo_contenido = models.CharField(max_length=20)
    tamano_bytes = models.BigIntegerField("tamaño (bytes)")
    fecha_registro = models.DateTimeField()

    class Meta:
        managed = False
        db_table = "fotos_deficiencia"
        verbose_name = "foto de deficiencia"
        verbose_name_plural = "fotos de deficiencias"

    def __str__(self):
        return self.nombre_archivo


class TareaOrden(models.Model):
    id_tarea = models.BigAutoField(primary_key=True)
    orden = models.ForeignKey(OrdenTrabajo, models.DO_NOTHING, db_column="id_orden", related_name="tareas")
    descripcion = models.CharField(max_length=200)
    completada = BitBooleanField(default=False)
    fecha_creacion = models.DateTimeField()
    fecha_completada = models.DateTimeField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = "tareas_orden"
        verbose_name = "tarea"
        verbose_name_plural = "tareas"

    def __str__(self):
        return self.descripcion


class Presupuesto(models.Model):
    class Estado(models.TextChoices):
        PENDIENTE = "PENDIENTE", "Pendiente"
        APROBADO = "APROBADO", "Aprobado"
        RECHAZADO = "RECHAZADO", "Rechazado"

    id_presupuesto = models.BigAutoField(primary_key=True)
    orden = models.ForeignKey(OrdenTrabajo, models.DO_NOTHING, db_column="id_orden", related_name="presupuestos")
    descripcion = models.CharField(max_length=500)
    estado = models.CharField(max_length=10, choices=Estado.choices)
    total = models.DecimalField(max_digits=10, decimal_places=2)
    fecha_creacion = models.DateTimeField()
    fecha_decision = models.DateTimeField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = "presupuestos"
        verbose_name = "presupuesto"
        verbose_name_plural = "presupuestos"
        ordering = ["-fecha_creacion"]

    def __str__(self):
        return f"Presupuesto {self.id_presupuesto} - S/ {self.total}"


class PresupuestoItem(models.Model):
    id_item = models.BigAutoField(primary_key=True)
    presupuesto = models.ForeignKey(Presupuesto, models.DO_NOTHING, db_column="id_presupuesto",
                                    related_name="items")
    tipo = models.CharField(max_length=10)
    descripcion = models.CharField(max_length=200)
    cantidad = models.IntegerField()
    precio_unitario = models.DecimalField(max_digits=10, decimal_places=2)
    subtotal = models.DecimalField(max_digits=10, decimal_places=2)

    class Meta:
        managed = False
        db_table = "presupuesto_items"
        verbose_name = "ítem del presupuesto"
        verbose_name_plural = "ítems del presupuesto"

    def __str__(self):
        return self.descripcion


# --------------------------------------------------------------------------
# Inventario
# --------------------------------------------------------------------------

class Material(models.Model):
    id_material = models.BigAutoField(primary_key=True)
    codigo = models.CharField(max_length=30, unique=True)
    nombre = models.CharField(max_length=100)
    unidad = models.CharField(max_length=20)
    costo_unitario = models.DecimalField(max_digits=10, decimal_places=2)
    stock_actual = models.IntegerField()
    stock_minimo = models.IntegerField("stock mínimo")
    version = models.BigIntegerField()

    class Meta:
        managed = False
        db_table = "materiales"
        verbose_name = "material"
        verbose_name_plural = "materiales"
        ordering = ["nombre"]

    def __str__(self):
        return f"{self.codigo} - {self.nombre}"

    @property
    def bajo_stock(self):
        return self.stock_actual <= self.stock_minimo


class SolicitudMaterial(models.Model):
    class Estado(models.TextChoices):
        PENDIENTE = "PENDIENTE", "Pendiente"
        APROBADA = "APROBADA", "Aprobada"
        RECHAZADA = "RECHAZADA", "Rechazada"

    id_solicitud = models.BigAutoField(primary_key=True)
    orden = models.ForeignKey(OrdenTrabajo, models.DO_NOTHING, db_column="id_orden", related_name="solicitudes")
    material = models.ForeignKey(Material, models.DO_NOTHING, db_column="id_material", related_name="solicitudes")
    cantidad = models.IntegerField()
    motivo = models.CharField(max_length=200, null=True, blank=True)
    estado = models.CharField(max_length=10, choices=Estado.choices)
    solicitado_por = models.CharField(max_length=100)
    fecha_solicitud = models.DateTimeField()
    atendido_por = models.CharField(max_length=100, null=True, blank=True)
    fecha_atencion = models.DateTimeField(null=True, blank=True)
    observacion = models.CharField(max_length=200, null=True, blank=True)

    class Meta:
        managed = False
        db_table = "solicitudes_material"
        verbose_name = "solicitud de repuesto"
        verbose_name_plural = "solicitudes de repuestos"
        ordering = ["-fecha_solicitud"]

    def __str__(self):
        return f"Solicitud {self.id_solicitud} - {self.material.codigo} x{self.cantidad}"


class MovimientoInventario(models.Model):
    class Tipo(models.TextChoices):
        ENTRADA = "ENTRADA", "Entrada"
        SALIDA = "SALIDA", "Salida"

    id_movimiento = models.BigAutoField(primary_key=True)
    material = models.ForeignKey(Material, models.DO_NOTHING, db_column="id_material", related_name="movimientos")
    tipo = models.CharField(max_length=10, choices=Tipo.choices)
    cantidad = models.IntegerField()
    costo_unitario = models.DecimalField(max_digits=10, decimal_places=2)
    stock_resultante = models.IntegerField()
    referencia = models.CharField(max_length=200, null=True, blank=True)
    orden = models.ForeignKey(OrdenTrabajo, models.DO_NOTHING, db_column="id_orden", null=True, blank=True,
                              related_name="movimientos")
    registrado_por = models.CharField(max_length=100)
    fecha = models.DateTimeField()

    class Meta:
        managed = False
        db_table = "movimientos_inventario"
        verbose_name = "movimiento de kardex"
        verbose_name_plural = "kardex (movimientos)"
        ordering = ["-fecha", "-id_movimiento"]

    def __str__(self):
        return f"{self.tipo} {self.material.codigo} x{self.cantidad}"

from django import forms
from django.contrib import admin

from .models import (Checklist, ChecklistItem, Cita, ConfiguracionTaller, Deficiencia, Especialidad,
                     FechaBloqueada, Feriado, HorarioMecanico, Material, MovimientoInventario,
                     OrdenTrabajo, Presupuesto, PresupuestoItem, Servicio, SolicitudMaterial,
                     TareaOrden, Usuario, Vehiculo)


class SoloLecturaAdmin(admin.ModelAdmin):
    """Tablas que gestiona Spring Boot: en el panel solo se consultan."""

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


class InlineSoloLectura(admin.TabularInline):
    """Detalle dentro de otra pagina, sin editar."""
    extra = 0
    can_delete = False

    def has_add_permission(self, request, obj=None):
        return False

    def has_change_permission(self, request, obj=None):
        return False


class SinBorrarAdmin(admin.ModelAdmin):
    """Catalogos editables que otras tablas usan: no se borran, se desactivan."""

    def has_delete_permission(self, request, obj=None):
        return False


def solo_mecanicos():
    return Usuario.objects.filter(rol__nombre="MECANICO", activo=True).order_by("nombre")


# --------------------------------------------------------------------------
# Solo lectura (se gestionan desde Spring Boot)
# --------------------------------------------------------------------------

@admin.register(Usuario)
class UsuarioAdmin(SoloLecturaAdmin):
    list_display = ("nombre", "correo", "rol", "telefono", "activo", "creado_en")
    list_filter = ("rol", "activo")
    search_fields = ("nombre", "correo", "telefono")
    exclude = ("password_hash",)
    list_select_related = ("rol",)


@admin.register(Vehiculo)
class VehiculoAdmin(SoloLecturaAdmin):
    list_display = ("placa", "marca", "modelo", "anio", "kilometraje", "cliente", "activo")
    list_filter = ("marca", "activo")
    search_fields = ("placa", "marca", "modelo", "cliente__nombre")
    list_select_related = ("cliente",)


@admin.register(Cita)
class CitaAdmin(SoloLecturaAdmin):
    list_display = ("ticket", "fecha", "hora_inicio", "cliente", "mecanico", "vehiculo", "servicio",
                    "estado", "precio")
    list_filter = ("estado", "fecha", "mecanico")
    search_fields = ("ticket", "cliente__nombre", "mecanico__nombre", "vehiculo__placa")
    date_hierarchy = "fecha"
    list_select_related = ("cliente", "mecanico", "vehiculo", "servicio")


# --------------------------------------------------------------------------
# Catalogos editables (con las mismas reglas basicas que valida Spring)
# --------------------------------------------------------------------------

class ServicioForm(forms.ModelForm):
    class Meta:
        model = Servicio
        fields = "__all__"

    def clean_precio_base(self):
        precio = self.cleaned_data["precio_base"]
        if precio <= 0:
            raise forms.ValidationError("El precio base debe ser mayor a 0.")
        return precio

    def clean_duracion_minutos(self):
        duracion = self.cleaned_data["duracion_minutos"]
        if duracion <= 0:
            raise forms.ValidationError("La duración debe ser mayor a 0 minutos.")
        return duracion


@admin.register(Servicio)
class ServicioAdmin(SinBorrarAdmin):
    form = ServicioForm
    list_display = ("nombre", "categoria", "precio_base", "duracion_minutos", "especialidad", "activo")
    list_filter = ("categoria", "activo", "especialidad")
    list_editable = ("activo",)
    search_fields = ("nombre", "descripcion")


@admin.register(Especialidad)
class EspecialidadAdmin(SinBorrarAdmin):
    list_display = ("nombre",)
    search_fields = ("nombre",)


class RangoHorasForm(forms.ModelForm):
    """Valida que la hora de inicio sea anterior a la hora de fin."""
    campo_inicio = "hora_inicio"
    campo_fin = "hora_fin"

    def clean(self):
        datos = super().clean()
        inicio, fin = datos.get(self.campo_inicio), datos.get(self.campo_fin)
        if inicio and fin and inicio >= fin:
            raise forms.ValidationError("La hora de inicio debe ser anterior a la hora de fin.")
        return datos


class HorarioMecanicoForm(RangoHorasForm):
    class Meta:
        model = HorarioMecanico
        fields = "__all__"


@admin.register(HorarioMecanico)
class HorarioMecanicoAdmin(admin.ModelAdmin):
    form = HorarioMecanicoForm
    list_display = ("mecanico", "dia_semana", "hora_inicio", "hora_fin")
    list_filter = ("dia_semana", "mecanico")
    list_select_related = ("mecanico",)

    def formfield_for_foreignkey(self, db_field, request, **kwargs):
        if db_field.name == "mecanico":
            kwargs["queryset"] = solo_mecanicos()
        return super().formfield_for_foreignkey(db_field, request, **kwargs)


class FechaBloqueadaForm(forms.ModelForm):
    class Meta:
        model = FechaBloqueada
        fields = "__all__"

    def clean(self):
        datos = super().clean()
        mecanico, fecha = datos.get("mecanico"), datos.get("fecha")
        if mecanico and fecha:
            repetida = FechaBloqueada.objects.filter(mecanico=mecanico, fecha=fecha)
            if self.instance.pk:
                repetida = repetida.exclude(pk=self.instance.pk)
            if repetida.exists():
                raise forms.ValidationError("Ese mecánico ya tiene bloqueada esa fecha.")
        return datos


@admin.register(FechaBloqueada)
class FechaBloqueadaAdmin(admin.ModelAdmin):
    form = FechaBloqueadaForm
    list_display = ("mecanico", "fecha", "motivo")
    list_filter = ("mecanico",)
    date_hierarchy = "fecha"
    list_select_related = ("mecanico",)

    def formfield_for_foreignkey(self, db_field, request, **kwargs):
        if db_field.name == "mecanico":
            kwargs["queryset"] = solo_mecanicos()
        return super().formfield_for_foreignkey(db_field, request, **kwargs)


@admin.register(Feriado)
class FeriadoAdmin(admin.ModelAdmin):
    list_display = ("fecha", "descripcion")
    date_hierarchy = "fecha"
    search_fields = ("descripcion",)


class ConfiguracionTallerForm(RangoHorasForm):
    campo_inicio = "hora_apertura"
    campo_fin = "hora_cierre"

    class Meta:
        model = ConfiguracionTaller
        fields = "__all__"


@admin.register(ConfiguracionTaller)
class ConfiguracionTallerAdmin(admin.ModelAdmin):
    """Los 7 dias ya existen: solo se edita si abre y en que horario."""
    form = ConfiguracionTallerForm
    list_display = ("dia_semana", "abierto", "hora_apertura", "hora_cierre")
    readonly_fields = ("dia_semana",)

    def has_add_permission(self, request):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


# --------------------------------------------------------------------------
# Ordenes de trabajo (solo lectura: el flujo y sus estados los controla Spring)
# --------------------------------------------------------------------------

class DeficienciaInline(InlineSoloLectura):
    model = Deficiencia
    fields = ("categoria", "severidad", "descripcion", "fotos", "fecha_registro")
    readonly_fields = fields

    @admin.display(description="fotos")
    def fotos(self, obj):
        return obj.fotos.count()


class TareaInline(InlineSoloLectura):
    model = TareaOrden
    fields = ("descripcion", "completada", "fecha_creacion", "fecha_completada")
    readonly_fields = fields


class PresupuestoInline(InlineSoloLectura):
    model = Presupuesto
    fields = ("descripcion", "estado", "total", "fecha_creacion", "fecha_decision")
    readonly_fields = fields
    show_change_link = True


class ConsumoInline(InlineSoloLectura):
    model = MovimientoInventario
    verbose_name = "repuesto consumido"
    verbose_name_plural = "repuestos consumidos"
    fields = ("material", "cantidad", "costo_unitario", "registrado_por", "fecha")
    readonly_fields = fields

    def get_queryset(self, request):
        return super().get_queryset(request).filter(tipo=MovimientoInventario.Tipo.SALIDA)


class SolicitudInline(InlineSoloLectura):
    model = SolicitudMaterial
    fields = ("material", "cantidad", "estado", "solicitado_por", "fecha_solicitud", "atendido_por")
    readonly_fields = fields


@admin.register(OrdenTrabajo)
class OrdenTrabajoAdmin(SoloLecturaAdmin):
    list_display = ("id_orden", "ticket", "estado", "mecanico", "placa", "fecha_inicio", "fecha_fin",
                    "costo_repuestos")
    list_filter = ("cita__estado", "cita__mecanico")
    search_fields = ("cita__ticket", "cita__vehiculo__placa", "cita__cliente__nombre")
    date_hierarchy = "fecha_inicio"
    list_select_related = ("cita", "cita__mecanico", "cita__vehiculo")
    inlines = (DeficienciaInline, TareaInline, PresupuestoInline, SolicitudInline, ConsumoInline)

    @admin.display(description="ticket", ordering="cita__ticket")
    def ticket(self, obj):
        return obj.cita.ticket

    @admin.display(description="estado", ordering="cita__estado")
    def estado(self, obj):
        return obj.cita.get_estado_display()

    @admin.display(description="mecánico")
    def mecanico(self, obj):
        return obj.cita.mecanico.nombre

    @admin.display(description="placa")
    def placa(self, obj):
        return obj.cita.vehiculo.placa


class ChecklistItemInline(InlineSoloLectura):
    model = ChecklistItem
    fields = ("elemento", "estado", "observacion")
    readonly_fields = fields


@admin.register(Checklist)
class ChecklistAdmin(SoloLecturaAdmin):
    list_display = ("orden", "fecha_registro", "observacion_general")
    list_select_related = ("orden", "orden__cita")
    inlines = (ChecklistItemInline,)


class PresupuestoItemInline(InlineSoloLectura):
    model = PresupuestoItem
    fields = ("tipo", "descripcion", "cantidad", "precio_unitario", "subtotal")
    readonly_fields = fields


@admin.register(Presupuesto)
class PresupuestoAdmin(SoloLecturaAdmin):
    list_display = ("id_presupuesto", "orden", "estado", "total", "fecha_creacion", "fecha_decision")
    list_filter = ("estado",)
    date_hierarchy = "fecha_creacion"
    list_select_related = ("orden", "orden__cita")
    inlines = (PresupuestoItemInline,)


# --------------------------------------------------------------------------
# Inventario (solo lectura: el stock usa @Version en Spring y solo cambia con el kardex)
# --------------------------------------------------------------------------

@admin.register(Material)
class MaterialAdmin(SoloLecturaAdmin):
    list_display = ("codigo", "nombre", "unidad", "costo_unitario", "stock_actual", "stock_minimo",
                    "alerta_stock")
    search_fields = ("codigo", "nombre")
    exclude = ("version",)

    @admin.display(description="stock OK", boolean=True)
    def alerta_stock(self, obj):
        return not obj.bajo_stock


@admin.register(MovimientoInventario)
class MovimientoInventarioAdmin(SoloLecturaAdmin):
    list_display = ("fecha", "material", "tipo", "cantidad", "costo_unitario", "stock_resultante", "orden",
                    "registrado_por", "referencia")
    list_filter = ("tipo", "material")
    search_fields = ("material__codigo", "material__nombre", "referencia", "registrado_por")
    date_hierarchy = "fecha"
    list_select_related = ("material", "orden", "orden__cita")


@admin.register(SolicitudMaterial)
class SolicitudMaterialAdmin(SoloLecturaAdmin):
    list_display = ("id_solicitud", "orden", "material", "cantidad", "estado", "solicitado_por",
                    "fecha_solicitud", "atendido_por")
    list_filter = ("estado",)
    search_fields = ("material__codigo", "material__nombre", "solicitado_por")
    date_hierarchy = "fecha_solicitud"
    list_select_related = ("orden", "orden__cita", "material")

from django import forms
from django.contrib import admin

from .models import (Cita, ConfiguracionTaller, Especialidad, FechaBloqueada, Feriado,
                     HorarioMecanico, Servicio, Usuario, Vehiculo)


class SoloLecturaAdmin(admin.ModelAdmin):
    """Tablas que gestiona Spring Boot: en el panel solo se consultan."""

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
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

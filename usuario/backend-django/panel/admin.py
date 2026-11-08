from django.contrib import admin

from .models import Cita, Usuario, Vehiculo


class SoloLecturaAdmin(admin.ModelAdmin):
    """Tablas que gestiona Spring Boot: en el panel solo se consultan."""

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


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

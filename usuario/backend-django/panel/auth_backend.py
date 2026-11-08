"""
Login del panel con las credenciales del ADMIN de AutoSmart.
Spring Boot guarda las contrasenas con BCrypt ($2a$...), el mismo formato que verifica la libreria bcrypt.
"""
import bcrypt
from django.contrib.auth.backends import BaseBackend
from django.contrib.auth.models import User

from .models import Usuario


class AutoSmartAdminBackend(BaseBackend):

    def authenticate(self, request, username=None, password=None, **kwargs):
        if not username or not password:
            return None

        usuario = (Usuario.objects.select_related("rol")
                   .filter(correo__iexact=username.strip(), rol__nombre="ADMIN")
                   .first())
        if usuario is None or not usuario.activo:
            return None

        try:
            valida = bcrypt.checkpw(password.encode("utf-8"), usuario.password_hash.encode("utf-8"))
        except ValueError:
            return None
        if not valida:
            return None

        # Usuario interno de Django para la sesion; la contrasena siempre se valida contra Spring
        user, creado = User.objects.get_or_create(
            username=usuario.correo.lower(),
            defaults={"email": usuario.correo, "first_name": usuario.nombre[:150]},
        )
        if creado:
            user.set_unusable_password()
        user.is_staff = True
        user.is_superuser = True
        user.is_active = True
        user.save()
        return user

    def get_user(self, user_id):
        try:
            return User.objects.get(pk=user_id)
        except User.DoesNotExist:
            return None

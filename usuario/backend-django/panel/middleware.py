"""Control de caidas del panel: si la base de datos no responde, se muestra una pagina clara (503)
en lugar del error tecnico de Django."""
import logging

from django.db import InterfaceError, OperationalError
from django.http import HttpResponse
from django.template import loader

logger = logging.getLogger("panel")


class BaseDeDatosCaidaMiddleware:

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        return self.get_response(request)

    def process_exception(self, request, exception):
        if isinstance(exception, (OperationalError, InterfaceError)):
            logger.error("La base de datos no responde (%s): %s", request.path, exception)
            # Se renderiza sin request para no volver a consultar la base (sesion, usuario)
            contenido = loader.render_to_string("503.html")
            return HttpResponse(contenido, status=503)
        return None  # cualquier otro error sigue su curso (500.html)

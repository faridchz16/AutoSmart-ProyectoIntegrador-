"""
Configuracion del panel de administracion de AutoSmart (Django 4.2 LTS).
Comparte la base autosmart_db con el backend Spring Boot.
"""
import os
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")

SECRET_KEY = os.getenv("DJANGO_SECRET_KEY", "solo-para-desarrollo")
# Por seguridad DEBUG queda apagado si no se indica lo contrario en .env:
# con DEBUG apagado nunca se muestran trazas, sino las paginas 404, 500 y 503 propias.
DEBUG = os.getenv("DJANGO_DEBUG", "False") == "True"
ALLOWED_HOSTS = ["localhost", "127.0.0.1"]

INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "panel",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
    # Control de caidas: base de datos sin conexion -> pagina 503
    "panel.middleware.BaseDeDatosCaidaMiddleware",
]

ROOT_URLCONF = "autosmart_admin.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.debug",
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "autosmart_admin.wsgi.application"

# Misma base de datos que Spring Boot (MariaDB 10.4 de XAMPP)
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.mysql",
        "NAME": os.getenv("DB_NAME", "autosmart_db"),
        "USER": os.getenv("DB_USER", "root"),
        "PASSWORD": os.getenv("DB_PASSWORD", ""),
        "HOST": os.getenv("DB_HOST", "127.0.0.1"),
        "PORT": os.getenv("DB_PORT", "3306"),
        # Timeouts: si MySQL esta caido o no responde, se falla en 5 s en lugar de quedarse esperando
        "OPTIONS": {"charset": "utf8mb4", "connect_timeout": 5, "read_timeout": 5, "write_timeout": 5},
    }
}

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

LANGUAGE_CODE = "es-pe"
TIME_ZONE = "America/Lima"
USE_I18N = True
# Spring guarda las fechas en hora de Lima sin zona horaria; asi Django las muestra igual
USE_TZ = False

STATIC_URL = "static/"
STATIC_ROOT = BASE_DIR / "staticfiles"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# Login: superusuarios de Django + ADMIN de AutoSmart con su misma contrasena de Spring
AUTHENTICATION_BACKENDS = [
    "django.contrib.auth.backends.ModelBackend",
    "panel.auth_backend.AutoSmartAdminBackend",
]

# Login: superusuarios de Django + ADMIN de AutoSmart con su misma contrasena de Spring
AUTHENTICATION_BACKENDS = [
    "django.contrib.auth.backends.ModelBackend",
    "panel.auth_backend.AutoSmartAdminBackend",
]

# Registro de errores en la consola (el detalle tecnico queda aqui y no se muestra al usuario)
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "handlers": {"consola": {"class": "logging.StreamHandler"}},
    "loggers": {
        "django.request": {"handlers": ["consola"], "level": "ERROR"},
        "panel": {"handlers": ["consola"], "level": "INFO"},
    },
}

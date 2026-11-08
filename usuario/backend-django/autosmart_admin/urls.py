from django.contrib import admin
from django.urls import path

# Titulos del panel de administracion
admin.site.site_header = "AutoSmart - Panel de administracion"
admin.site.site_title = "AutoSmart Admin"
admin.site.index_title = "Gestion del taller"

urlpatterns = [
    path("admin/", admin.site.urls),
]

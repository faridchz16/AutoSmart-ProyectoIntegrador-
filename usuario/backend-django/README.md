# Panel de administracion AutoSmart (Django)

Panel de administracion que comparte la base `autosmart_db` con el backend Spring Boot.

- Python 3.12 (`uv venv venv --python 3.12`)
- Django 4.2 LTS: compatible con MariaDB 10.4 (XAMPP). Django 5.x exige MariaDB 10.5+.
- Configuracion en `.env` (copiar desde `.env.example`).

## Levantar el panel

1. `source venv/Scripts/activate`
2. `uv pip install -r requirements.txt`
3. `python manage.py runserver 8000`

Panel: http://localhost:8000/admin

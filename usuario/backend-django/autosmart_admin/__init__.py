import pymysql

# PyMySQL reemplaza a mysqlclient (no requiere compilar en Windows)
pymysql.install_as_MySQLdb()

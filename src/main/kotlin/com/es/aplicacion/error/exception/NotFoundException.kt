package com.es.aplicacion.error.exception

class NotFoundException (message: String)
    : Exception("Not found exception (404). $message") {

}
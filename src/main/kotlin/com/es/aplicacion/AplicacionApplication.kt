package com.es.aplicacion

import com.es.aplicacion.security.RSAKeysProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(RSAKeysProperties::class)
class AplicacionApplication

fun main(args: Array<String>) {
	runApplication<AplicacionApplication>(*args)
}

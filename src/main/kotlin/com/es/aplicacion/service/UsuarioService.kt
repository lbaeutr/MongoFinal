package com.es.aplicacion.service

import com.es.aplicacion.dto.UsuarioDTO
import com.es.aplicacion.dto.UsuarioRegisterDTO
import com.es.aplicacion.error.exception.BadRequestException
import com.es.aplicacion.error.exception.UnauthorizedException
import com.es.aplicacion.model.Usuario
import com.es.aplicacion.repository.UsuarioRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UsuarioService : UserDetailsService {

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    private lateinit var externalApiService: ExternalApiService


    override fun loadUserByUsername(username: String?): UserDetails {
        var usuario: Usuario = usuarioRepository
            .findByUsername(username!!)
            .orElseThrow {
                UnauthorizedException("$username no existente")
            }

        return User.builder()
            .username(usuario.username)
            .password(usuario.password)
            .roles(usuario.roles)
            .build()
    }

    fun insertUser(usuarioInsertadoDTO: UsuarioRegisterDTO) : UsuarioDTO {

        // COMPROBACIONES
        // Comprobar si los campos vienen vacíos
        if (usuarioInsertadoDTO.username.isBlank()
            || usuarioInsertadoDTO.email.isBlank()
            || usuarioInsertadoDTO.password.isBlank()
            || usuarioInsertadoDTO.passwordRepeat.isBlank()) {

            throw BadRequestException("Uno o más campos vacíos")
        }

        // Fran ha comprobado que el usuario existe previamente
        if(usuarioRepository.findByUsername(usuarioInsertadoDTO.username).isPresent) {
            throw Exception("Usuario ${usuarioInsertadoDTO.username} ya está registrado")
        }

        // comprobar que ambas passwords sean iguales
        if(usuarioInsertadoDTO.password != usuarioInsertadoDTO.passwordRepeat) {
            throw BadRequestException("Las contrasenias no coinciden")
        }

        // Comprobar el ROL
        if(usuarioInsertadoDTO.rol != null && usuarioInsertadoDTO.rol != "USER" && usuarioInsertadoDTO.rol != "ADMIN" ) {
            throw BadRequestException("ROL: ${usuarioInsertadoDTO.rol} incorrecto")
        }

        // Comprobar el EMAIL


        // Comprobar la provincia
        val datosProvincias = externalApiService.obtenerProvinciasDesdeApi()
        var cpro: String = ""
        if(datosProvincias != null) {
            if(datosProvincias.data != null) {
                val provinciaEncontrada = datosProvincias.data.stream().filter {
                    it.PRO == usuarioInsertadoDTO.direccion.provincia.uppercase()
                }.findFirst().orElseThrow {
                    BadRequestException("Provincia ${usuarioInsertadoDTO.direccion.provincia} no encontrada")
                }
                cpro = provinciaEncontrada.CPRO
            }
        }

        // Comprobar el municipio
        val datosMunicipios = externalApiService.obtenerMunicipiosDesdeApi(cpro)
        if(datosMunicipios != null) {
            if(datosMunicipios.data != null) {
                datosMunicipios.data.stream().filter {
                    it.DMUN50 == usuarioInsertadoDTO.direccion.municipio.uppercase()
                }.findFirst().orElseThrow {
                    BadRequestException("Municipio ${usuarioInsertadoDTO.direccion.municipio} incorrecto")
                }
            }
        }

        // Insertar el user (convierto a Entity)
        val usuario = Usuario(
             null,
            usuarioInsertadoDTO.username,
            passwordEncoder.encode(usuarioInsertadoDTO.password),
            usuarioInsertadoDTO.email,
            usuarioInsertadoDTO.rol,
            usuarioInsertadoDTO.direccion
        )

        // inserto en bd
        usuarioRepository.insert(usuario)

        // retorno un DTO
        return UsuarioDTO(
            usuario.username,
            usuario.email,
            usuario.roles
        )

    }

    // Funciones de gestión de usuarios , incorporadas por mi
    fun getAllUsers(): List<UsuarioDTO> {
        return usuarioRepository.findAll().map { usuario ->
            UsuarioDTO(usuario.username, usuario.email, usuario.roles)
        }
    }

    fun getUserByUsername(username: String): UsuarioDTO {
        val usuario = usuarioRepository.findByUsername(username).orElseThrow {
            UnauthorizedException("$username no existente")
        }
        return UsuarioDTO(usuario.username, usuario.email, usuario.roles)
    }

    fun updateUser(username: String, usuarioUpdateDTO: UsuarioRegisterDTO): UsuarioDTO {
        val usuario = usuarioRepository.findByUsername(username).orElseThrow {
            UnauthorizedException("$username no existente")
        }

        val updatedUsuario = usuario.copy(
            username = usuarioUpdateDTO.username,
            password = passwordEncoder.encode(usuarioUpdateDTO.password),
            email = usuarioUpdateDTO.email,
            roles = usuarioUpdateDTO.rol,
            direccion = usuarioUpdateDTO.direccion
        )

        usuarioRepository.save(updatedUsuario)

        return UsuarioDTO(updatedUsuario.username, updatedUsuario.email, updatedUsuario.roles)
    }

    fun deleteUser(username: String) {
        val usuario = usuarioRepository.findByUsername(username).orElseThrow {
            UnauthorizedException("$username no existente")
        }
        usuarioRepository.delete(usuario)
    }
}
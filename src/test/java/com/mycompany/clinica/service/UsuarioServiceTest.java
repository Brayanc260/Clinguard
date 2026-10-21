package com.mycompany.clinica.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void obtenerPorNombre_existente_retornaUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("brayan");
        when(usuarioRepo.findByNombreUsuario("brayan")).thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioService.obtenerPorNombre("brayan");

        assertThat(resultado.getNombreUsuario()).isEqualTo("brayan");
    }

    @Test
    void obtenerPorNombre_noExiste_lanzaExcepcion() {
        when(usuarioRepo.findByNombreUsuario("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorNombre("nadie"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }

    @Test
    void registrar_usuarioNuevo_guardaConPasswordEncriptada() {
        when(usuarioRepo.findByNombreUsuario("nuevo")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("hash1234");
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.registrar("nuevo", "1234", Rol.MEDICO);

        assertThat(resultado.getNombreUsuario()).isEqualTo("nuevo");
        assertThat(resultado.getPassword()).isEqualTo("hash1234");
        assertThat(resultado.getRol()).isEqualTo(Rol.MEDICO);
        verify(usuarioRepo).save(any(Usuario.class));
    }

    @Test
    void registrar_usuarioExistente_lanzaExcepcionYNoGuarda() {
        when(usuarioRepo.findByNombreUsuario("brayan")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> usuarioService.registrar("brayan", "1234", Rol.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre de usuario ya está en uso");

        verify(usuarioRepo, never()).save(any(Usuario.class));
    }
}

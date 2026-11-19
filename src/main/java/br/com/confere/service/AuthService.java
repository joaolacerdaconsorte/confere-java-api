package br.com.confere.service;

import br.com.confere.domain.entity.Municipio;
import br.com.confere.domain.entity.Usuario;
import br.com.confere.domain.enums.PerfilUsuario;
import br.com.confere.dto.*;
import br.com.confere.exception.ConfereException;
import br.com.confere.repository.MunicipioRepository;
import br.com.confere.repository.UsuarioRepository;
import br.com.confere.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final MunicipioRepository municipioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public RegistrarResponse registrar(RegistrarRequest request) {
        String emailNorm = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(emailNorm)) {
            throw new ConfereException(HttpStatus.CONFLICT, "CONFLITO", "Já existe uma conta cadastrada com este e-mail.");
        }

        Municipio municipio = municipioRepository.findByCodIbge(request.codIbgeMunicipio())
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO",
                        "Município não encontrado para o código IBGE " + request.codIbgeMunicipio()));

        Usuario usuario = Usuario.builder()
                .nome(request.nome().trim())
                .email(emailNorm)
                .telefone(request.telefone())
                .senhaHash(passwordEncoder.encode(request.senha()))
                .perfil(PerfilUsuario.CIDADAO)
                .dtNascimento(request.dtNascimento())
                .municipio(municipio)
                .fonteGrande(false)
                .audioAtivo(true)
                .ativo(true)
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        return new RegistrarResponse(salvo.getId(), salvo.getNome(), salvo.getEmail());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String emailNorm = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(emailNorm, request.senha())
        );

        Usuario usuario = usuarioRepository.findByEmailIgnoreCaseWithMunicipio(emailNorm)
                .orElseThrow(() -> new ConfereException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "E-mail ou senha incorretos."));

        String token = jwtService.gerarToken(usuario);
        OffsetDateTime expiraEm = OffsetDateTime.now(TimeUtils.SAO_PAULO_ZONE)
                .plusSeconds(jwtService.getExpirationSeconds());

        return new LoginResponse(
                token,
                "Bearer",
                expiraEm,
                UsuarioService.toResumoDto(usuario)
        );
    }
}

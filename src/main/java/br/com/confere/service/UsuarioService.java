package br.com.confere.service;

import br.com.confere.domain.entity.Municipio;
import br.com.confere.domain.entity.Usuario;
import br.com.confere.dto.AtualizarPerfilRequest;
import br.com.confere.dto.MunicipioDto;
import br.com.confere.dto.PushTokenRequest;
import br.com.confere.dto.UsuarioResumoDto;
import br.com.confere.exception.ConfereException;
import br.com.confere.repository.MunicipioRepository;
import br.com.confere.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final MunicipioRepository municipioRepository;

    @Transactional(readOnly = true)
    public UsuarioResumoDto buscarPerfil(Long usuarioId) {
        Usuario usuario = usuarioRepository.findByIdWithMunicipio(usuarioId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Usuário não encontrado."));
        return toResumoDto(usuario);
    }

    @Transactional
    public UsuarioResumoDto atualizarPerfil(Long usuarioId, AtualizarPerfilRequest request) {
        Usuario usuario = usuarioRepository.findByIdWithMunicipio(usuarioId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Usuário não encontrado."));

        if (request.nome() != null && !request.nome().isBlank()) {
            usuario.setNome(request.nome().trim());
        }
        if (request.telefone() != null) {
            usuario.setTelefone(request.telefone());
        }
        if (request.dtNascimento() != null) {
            usuario.setDtNascimento(request.dtNascimento());
        }
        if (request.codIbgeMunicipio() != null) {
            Municipio municipio = municipioRepository.findByCodIbge(request.codIbgeMunicipio())
                    .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Município não encontrado."));
            usuario.setMunicipio(municipio);
        }
        if (request.fonteGrande() != null) {
            usuario.setFonteGrande(request.fonteGrande());
        }
        if (request.audioAtivo() != null) {
            usuario.setAudioAtivo(request.audioAtivo());
        }

        return toResumoDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public void atualizarPushToken(Long usuarioId, PushTokenRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Usuário não encontrado."));
        usuario.setExpoPushToken(request.expoPushToken().trim());
        usuarioRepository.save(usuario);
    }

    public static UsuarioResumoDto toResumoDto(Usuario usuario) {
        Municipio m = usuario.getMunicipio();
        return new UsuarioResumoDto(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil().name(),
                new MunicipioDto(m.getCodIbge(), m.getNome(), m.getUf()),
                Boolean.TRUE.equals(usuario.getFonteGrande()),
                Boolean.TRUE.equals(usuario.getAudioAtivo())
        );
    }
}

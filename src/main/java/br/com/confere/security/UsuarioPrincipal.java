package br.com.confere.security;

import br.com.confere.domain.entity.Usuario;
import br.com.confere.domain.enums.PerfilUsuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final PerfilUsuario perfil;
    private final Integer codIbgeMunicipio;
    private final boolean ativo;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
        this.perfil = usuario.getPerfil();
        this.codIbgeMunicipio = usuario.getMunicipio().getCodIbge();
        this.ativo = Boolean.TRUE.equals(usuario.getAtivo());
    }

    public UsuarioPrincipal(Long id, String nome, String email, PerfilUsuario perfil, Integer codIbgeMunicipio) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = "";
        this.perfil = perfil;
        this.codIbgeMunicipio = codIbgeMunicipio;
        this.ativo = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}

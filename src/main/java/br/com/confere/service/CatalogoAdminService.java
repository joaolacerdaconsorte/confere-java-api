package br.com.confere.service;

import br.com.confere.domain.entity.RegraOffline;
import br.com.confere.domain.entity.TipoGolpe;
import br.com.confere.dto.*;
import br.com.confere.exception.ConfereException;
import br.com.confere.repository.CanalOrigemRepository;
import br.com.confere.repository.RegraOfflineRepository;
import br.com.confere.repository.TipoGolpeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoAdminService {

    private final CanalOrigemRepository canalOrigemRepository;
    private final TipoGolpeRepository tipoGolpeRepository;
    private final RegraOfflineRepository regraOfflineRepository;

    @Transactional(readOnly = true)
    public List<CanalOrigemDto> listarCanais() {
        return canalOrigemRepository.findAll().stream()
                .map(c -> new CanalOrigemDto(c.getId(), c.getCodigo(), c.getNome()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TipoGolpeDto> listarTiposGolpe() {
        return tipoGolpeRepository.findAll().stream()
                .map(this::toTipoGolpeDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<TipoGolpeDto> listarTiposGolpePaginado(int pagina, int tamanho) {
        int tam = Math.min(Math.max(tamanho, 1), 50);
        Page<TipoGolpe> page = tipoGolpeRepository.findAll(PageRequest.of(Math.max(pagina, 0), tam));
        return PaginaResponse.of(page.map(this::toTipoGolpeDto));
    }

    @Transactional(readOnly = true)
    public TipoGolpeDto buscarTipoGolpe(Long id) {
        TipoGolpe tipo = tipoGolpeRepository.findById(id)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Tipo de golpe não encontrado."));
        return toTipoGolpeDto(tipo);
    }

    @Transactional
    public TipoGolpeDto criarTipoGolpe(TipoGolpeRequest request) {
        if (tipoGolpeRepository.existsByCodigoIgnoreCase(request.codigo())) {
            throw new ConfereException(HttpStatus.CONFLICT, "CONFLITO", "Já existe tipo de golpe com o código " + request.codigo());
        }
        TipoGolpe salvo = tipoGolpeRepository.save(TipoGolpe.builder()
                .codigo(request.codigo().trim().toUpperCase())
                .nome(request.nome().trim())
                .descricao(request.descricao())
                .gravidade(request.gravidade())
                .orgaoImitado(request.orgaoImitado())
                .build());
        return toTipoGolpeDto(salvo);
    }

    @Transactional
    public TipoGolpeDto atualizarTipoGolpe(Long id, TipoGolpeRequest request) {
        TipoGolpe tipo = tipoGolpeRepository.findById(id)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Tipo de golpe não encontrado."));

        tipo.setCodigo(request.codigo().trim().toUpperCase());
        tipo.setNome(request.nome().trim());
        tipo.setDescricao(request.descricao());
        tipo.setGravidade(request.gravidade());
        tipo.setOrgaoImitado(request.orgaoImitado());
        return toTipoGolpeDto(tipoGolpeRepository.save(tipo));
    }

    @Transactional
    public void excluirTipoGolpe(Long id) {
        if (!tipoGolpeRepository.existsById(id)) {
            throw new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Tipo de golpe não encontrado.");
        }
        tipoGolpeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public RegrasOfflineResponse listarRegrasOfflineAtivas(Integer versaoCliente) {
        Integer versaoAtual = regraOfflineRepository.findMaxVersaoAtiva();
        if (versaoCliente != null && versaoCliente.equals(versaoAtual)) {
            return null;
        }
        List<RegraOfflineDto> regras = regraOfflineRepository.findByAtivoTrueOrderByIdAsc().stream()
                .map(this::toRegraDto)
                .toList();
        return new RegrasOfflineResponse(versaoAtual, regras);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<RegraOfflineDto> listarTodasRegrasOffline(int pagina, int tamanho) {
        int tam = Math.min(Math.max(tamanho, 1), 50);
        Page<RegraOffline> page = regraOfflineRepository.findAll(PageRequest.of(Math.max(pagina, 0), tam));
        return PaginaResponse.of(page.map(this::toRegraDto));
    }

    @Transactional(readOnly = true)
    public RegraOfflineDto buscarRegraOffline(Long id) {
        RegraOffline regra = regraOfflineRepository.findById(id)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Regra offline não encontrada."));
        return toRegraDto(regra);
    }

    @Transactional
    public RegraOfflineDto criarRegraOffline(RegraOfflineRequest request) {
        int novaVersao = regraOfflineRepository.findMaxVersaoAtiva() + 1;
        RegraOffline salva = regraOfflineRepository.save(RegraOffline.builder()
                .padrao(request.padrao().trim())
                .tipoSinal(request.tipoSinal().trim().toUpperCase())
                .peso(request.peso())
                .mensagem(request.mensagem().trim())
                .versao(novaVersao)
                .ativo(request.ativo() != null ? request.ativo() : true)
                .build());
        return toRegraDto(salva);
    }

    @Transactional
    public RegraOfflineDto atualizarRegraOffline(Long id, RegraOfflineRequest request) {
        RegraOffline regra = regraOfflineRepository.findById(id)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Regra offline não encontrada."));

        int novaVersao = regraOfflineRepository.findMaxVersaoAtiva() + 1;
        regra.setPadrao(request.padrao().trim());
        regra.setTipoSinal(request.tipoSinal().trim().toUpperCase());
        regra.setPeso(request.peso());
        regra.setMensagem(request.mensagem().trim());
        regra.setVersao(novaVersao);
        if (request.ativo() != null) {
            regra.setAtivo(request.ativo());
        }
        return toRegraDto(regraOfflineRepository.save(regra));
    }

    @Transactional
    public void excluirRegraOffline(Long id) {
        if (!regraOfflineRepository.existsById(id)) {
            throw new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Regra offline não encontrada.");
        }
        regraOfflineRepository.deleteById(id);
    }

    private TipoGolpeDto toTipoGolpeDto(TipoGolpe t) {
        return new TipoGolpeDto(t.getId(), t.getCodigo(), t.getNome(), t.getDescricao(), t.getGravidade(), t.getOrgaoImitado());
    }

    private RegraOfflineDto toRegraDto(RegraOffline r) {
        return new RegraOfflineDto(r.getId(), r.getPadrao(), r.getTipoSinal(), r.getPeso(), r.getMensagem(),
                r.getVersao(), Boolean.TRUE.equals(r.getAtivo()));
    }
}

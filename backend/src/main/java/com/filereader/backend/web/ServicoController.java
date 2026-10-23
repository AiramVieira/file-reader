package com.filereader.backend.web;

import com.filereader.backend.domain.Servico;
import com.filereader.backend.repository.ServicoRepository;
import com.filereader.backend.web.dto.AtualizarServicoRequest;
import com.filereader.backend.web.dto.CriarServicoRequest;
import com.filereader.backend.web.dto.ServicoDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoRepository servicoRepository;

    public ServicoController(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    @GetMapping
    public List<ServicoDTO> listar() {
        return servicoRepository.findAll().stream().map(ServicoDTO::of).toList();
    }

    @GetMapping("/{id}")
    public ServicoDTO buscar(@PathVariable Long id) {
        return ServicoDTO.of(buscarOuFalhar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoDTO criar(@Valid @RequestBody CriarServicoRequest request) {
        Servico servico = new Servico();
        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setAtivo(true);
        return ServicoDTO.of(servicoRepository.save(servico));
    }

    @PutMapping("/{id}")
    public ServicoDTO atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarServicoRequest request) {
        Servico servico = buscarOuFalhar(id);
        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setAtivo(request.ativo());
        return ServicoDTO.of(servicoRepository.save(servico));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        if (!servicoRepository.existsById(id)) {
            throw new EntityNotFoundException("Servico nao encontrado: " + id);
        }
        servicoRepository.deleteById(id);
    }

    private Servico buscarOuFalhar(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Servico nao encontrado: " + id));
    }
}

package br.edu.pucgoias.ads1253.clinica.service;

import br.edu.pucgoias.ads1253.clinica.model.Animal;
import br.edu.pucgoias.ads1253.clinica.model.Tutor;
import br.edu.pucgoias.ads1253.clinica.repository.AnimalRepository;
import br.edu.pucgoias.ads1253.clinica.repository.TutorRepository;
import br.edu.pucgoias.ads1253.clinica.service.exception.CpfDuplicadoException;
import br.edu.pucgoias.ads1253.clinica.service.exception.RecursoNaoEncontradoException;
import br.edu.pucgoias.ads1253.clinica.service.exception.TutorComAnimaisException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicaService {

    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;

    public ClinicaService(TutorRepository tutorRepository, AnimalRepository animalRepository) {
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
    }

    // --- Tutor --------------------------------------------------------------

    @Transactional
    public Tutor cadastrarTutor(Tutor tutor) {
        if (tutorRepository.existsByCpf(tutor.getCpf())) {
            throw new CpfDuplicadoException(tutor.getCpf());
        }
        return tutorRepository.save(tutor);
    }

    @Transactional(readOnly = true)
    public Tutor buscarTutor(Long id) {
        return tutorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor", id));
    }

    @Transactional(readOnly = true)
    public List<Tutor> listarTutores() {
        return tutorRepository.findAll();
    }

    /** Atualiza nome e telefone do tutor; identificador inexistente deve falhar. */
    @Transactional
    public Tutor atualizarTutor(Long id, String novoNome, String novoTelefone) {
        Tutor tutor = buscarTutor(id);
        tutor.setNome(novoNome);
        tutor.setTelefone(novoTelefone);
        return tutorRepository.save(tutor);
    }

    /** Remove o tutor apenas se nao houver animais vinculados. */
    @Transactional
    public void removerTutor(Long id) {
        Tutor tutor = buscarTutor(id);
        long quantidadeAnimais = animalRepository.countByTutorId(id);
        if (quantidadeAnimais > 0) {
            throw new TutorComAnimaisException(id, quantidadeAnimais);
        }
        tutorRepository.delete(tutor);
    }

    // --- Animal -------------------------------------------------------------

    /** Cadastra o animal vinculando-o ao tutor informado. */
    @Transactional
    public Animal cadastrarAnimal(Long tutorId, Animal animal) {
        Tutor tutor = buscarTutor(tutorId);
        tutor.adicionarAnimal(animal);
        return animalRepository.save(animal);
    }

    @Transactional(readOnly = true)
    public List<Animal> listarAnimaisDoTutor(Long tutorId) {
        garantirQueTutorExiste(tutorId);
        return animalRepository.findByTutorId(tutorId);
    }

    @Transactional(readOnly = true)
    public List<Animal> buscarAnimaisPorEspecie(String especie) {
        return animalRepository.findByEspecieIgnoreCase(especie);
    }

    @Transactional
    public void removerAnimal(Long animalId) {
        Animal animal = buscarAnimal(animalId);
        animal.getTutor().getAnimais().remove(animal);
        animalRepository.delete(animal);
    }

    // --- Auxiliares ---------------------------------------------------------

    private Animal buscarAnimal(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Animal", id));
    }

    private void garantirQueTutorExiste(Long tutorId) {
        if (!tutorRepository.existsById(tutorId)) {
            throw new RecursoNaoEncontradoException("Tutor", tutorId);
        }
    }
}
package be.axxes.timesheets.service;

import be.axxes.timesheets.dto.ProjectDto;
import be.axxes.timesheets.model.Project;
import be.axxes.timesheets.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<Project> getAllActiveProjects() {
        return projectRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project getById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project niet gevonden: " + id));
    }

    public ProjectDto toDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getDailyHourTarget(),
                project.isBillable(),
                project.isActive()
        );
    }

    @Transactional
    public Project create(ProjectDto dto) {
        var project = new Project();
        applyDto(project, dto);
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(Long id, ProjectDto dto) {
        var project = getById(id);
        applyDto(project, dto);
        return projectRepository.save(project);
    }

    @Transactional
    public void archive(Long id) {
        var project = getById(id);
        project.setActive(false);
        projectRepository.save(project);
    }

    @Transactional
    public void activate(Long id) {
        var project = getById(id);
        project.setActive(true);
        projectRepository.save(project);
    }

    private void applyDto(Project project, ProjectDto dto) {
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setDailyHourTarget(dto.getDailyHourTarget());
        project.setBillable(dto.isBillable());
        project.setActive(dto.isActive());
    }
}
